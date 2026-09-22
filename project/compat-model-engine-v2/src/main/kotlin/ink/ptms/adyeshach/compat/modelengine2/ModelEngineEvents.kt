package ink.ptms.adyeshach.compat.modelengine2

import com.ticxo.modelengine.api.ModelEngineAPI
import ink.ptms.adyeshach.compat.modelengine2.DefaultModelEngine.Companion.isModelEngineHooked
import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.EntityInstance
import ink.ptms.adyeshach.core.entity.ModelEngine
import ink.ptms.adyeshach.core.event.AdyeshachEntityDamageEvent
import ink.ptms.adyeshach.core.event.AdyeshachEntityInteractEvent
import ink.ptms.adyeshach.core.util.safeDistance
import ink.ptms.adyeshach.impl.util.RayTrace
import ink.ptms.adyeshach.impl.util.runOnRegion
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.util.Vector
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.navigation.BoundingBox
import taboolib.platform.util.isMainhand

internal object ModelEngineEvents {

    @Awake(LifeCycle.ENABLE)
    fun init() {
        if (isModelEngineHooked) {
            // 名称变动
            Adyeshach.api().getEventBus().prepareMetaUpdate { e ->
                val entity = e.entity as? ModelEngine ?: return@prepareMetaUpdate true
                if (e.key == "customName" || e.key == "isCustomNameVisible") {
                    (entity as? EntityInstance)?.runOnRegion(delay = 1) { entity.updateModelEngineNameTag() }
                        ?: entity.updateModelEngineNameTag()
                }
                true
            }
            // 移动状态变动
            Adyeshach.api().getEventBus().prepareMove { e ->
                val modelManager = ModelEngineAPI.api.modelManager
                val entity = e.entity as? ModelEngine ?: return@prepareMove
                if (entity.modelEngineUniqueId != null) {
                    modelManager.getModeledEntity(entity.modelEngineUniqueId)?.isWalking = e.isMoving
                }
            }
        }
    }

    @SubscribeEvent
    private fun onInteract(e: PlayerInteractEvent) {
        if (isModelEngineHooked && e.action != Action.PHYSICAL) {
            val modelManager = ModelEngineAPI.api.modelManager
            val entities = ArrayList<Pair<EntityInstance, BoundingBox>>()
            Adyeshach.api().getEntityFinder().getEntities(e.player) { it.getLocation().safeDistance(e.player.location) <= 5 }.forEach {
                if (it !is ModelEngine) {
                    return@forEach
                }
                if (it.modelEngineUniqueId != null) {
                    val modeledEntity = modelManager.getModeledEntity(it.modelEngineUniqueId) ?: return@forEach
                    val blueprint = modeledEntity.getActiveModel(it.modelEngineName).blueprint ?: return@forEach
                    val boundingBoxHeight = blueprint.boundingBoxHeight
                    val boundingBoxWidth = blueprint.boundingBoxWidth / 2
                    val location = it.getLocation()
                    entities += it to BoundingBox(
                        location.x - boundingBoxWidth,
                        location.y,
                        location.z - boundingBoxWidth,
                        location.x + boundingBoxWidth,
                        location.y + boundingBoxHeight,
                        location.z + boundingBoxWidth,
                    )
                }
            }
            RayTrace(e.player).traces(5.0, 0.2).forEach { vec ->
                entities.filter { it.second.contains(vec) && it.first.isVisibleViewer(e.player) }.forEach {
                    val result = if (e.action == Action.LEFT_CLICK_AIR || e.action == Action.LEFT_CLICK_BLOCK) {
                        AdyeshachEntityDamageEvent(it.first, e.player).call()
                    } else {
                        AdyeshachEntityInteractEvent(it.first, e.player, e.isMainhand(), Vector(vec.x, vec.y, vec.z)).call()
                    }
                    if (result) {
                        return
                    }
                }
            }
        }
    }
}
