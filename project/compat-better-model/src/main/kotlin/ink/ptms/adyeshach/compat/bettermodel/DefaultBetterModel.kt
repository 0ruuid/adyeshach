package ink.ptms.adyeshach.compat.bettermodel

import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.AdyeshachEntityTypeRegistry
import ink.ptms.adyeshach.core.entity.BetterModelView
import ink.ptms.adyeshach.core.entity.EntityInstance
import ink.ptms.adyeshach.core.entity.EntityTypes
import ink.ptms.adyeshach.core.entity.type.AdyInteraction
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.bone.BoneTags
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter
import kr.toxicity.model.api.tracker.DummyTracker
import kr.toxicity.model.api.tracker.TrackerModifier
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy

internal interface DefaultBetterModel : AdyInteraction, BetterModelView {

    override var betterModelName: String
        get() {
            val entity = this as EntityInstance
            return entity.getPersistentTag("BetterModel:Name") ?: ""
        }
        set(value) {
            val entity = this as EntityInstance
            if (value.isBlank()) {
                entity.removePersistentTag("BetterModel:Name")
                destroyModel()
            } else {
                entity.setPersistentTag("BetterModel:Name", value)
                refreshModel()
            }
        }

    override fun showModel(viewer: Player): Boolean {
        if (!isBetterModelHooked() || betterModelName.isBlank()) return false
        val entity = this as EntityInstance
        val tracker = ensureDummy()
        tracker?.let {
            setHitbox(it)
            entity.updateEntityMetadata(viewer)
            val player = BukkitAdapter.adapt(viewer)
            it.spawn(player)
            it.show(player)
        }
        return true
    }

    override fun hideModel(viewer: Player): Boolean {
        if (!isBetterModelHooked() || betterModelName.isBlank()) return false
        getDummy()?.hide(BukkitAdapter.adapt(viewer))
        return true
    }

    override fun destroyModel() {
        getDummy()?.close()
        setDummy(null)
    }

    override fun teleportModel(location: Location) {
        if (!isBetterModelHooked() || betterModelName.isBlank()) return
        val tracker = ensureDummy() ?: return
        tracker.location(BukkitAdapter.adapt(location))
    }

    override fun refreshModel(): Boolean {
        if (!isBetterModelHooked()) return false
        if (betterModelName.isBlank()) {
            destroyModel()
            return true
        }
        val entity = this as EntityInstance
        val existed = getDummy()
        val tracker = ensureDummy() ?: return true
        tracker.location(BukkitAdapter.adapt(entity.getLocation()))
        setHitbox(tracker)
        entity.getVisiblePlayers().forEach { entity.updateEntityMetadata(it) }
        if (existed == null) {
            entity.getVisiblePlayers().forEach {
                val player = BukkitAdapter.adapt(it)
                tracker.spawn(player)
                tracker.show(player)
            }
        }
        return true
    }

    override fun updateModelNameTag() {}

    override fun playIdle() {
        getDummy()?.animate("idle", AnimationModifier(0, Int.MAX_VALUE, AnimationIterator.Type.LOOP))
    }

    override fun playWalk() {
        getDummy()?.animate("walk", AnimationModifier(0, Int.MAX_VALUE, AnimationIterator.Type.LOOP))
    }

    private fun ensureDummy(): DummyTracker? {
        val entity = this as EntityInstance
        val exist = getDummy()
        if (exist != null) return exist
        val renderer = BetterModel.model(betterModelName).orElse(null)
        if (renderer == null) {
            warning("Cannot find BetterModel: $betterModelName")
            return null
        }
        val created = renderer.create(BukkitAdapter.adapt(entity.getLocation()), TrackerModifier.DEFAULT)
        setDummy(created)
        return created
    }

    private fun getDummy(): DummyTracker? {
        val entity = this as EntityInstance
        return entity.getTag("BetterModel:DummyTracker") as? DummyTracker
    }

    private fun setDummy(tracker: DummyTracker?) {
        val entity = this as EntityInstance
        entity.setTag("BetterModel:DummyTracker", tracker)
    }

    private fun setHitbox(tracker: DummyTracker) {
        val pipeline = tracker.pipeline
        val bones = pipeline.bones()
        val hitboxBone = bones.firstOrNull { bone ->
            bone.name().name().equals("hitbox", ignoreCase = true) ||
                bone.name().tagged(BoneTags.HITBOX)
        }
        val boundingBox = hitboxBone?.group?.hitBox
        if (boundingBox != null) {
            val scale = hitboxBone.hitBoxScale()
            val width = ((boundingBox.x() + boundingBox.z()) / 2.0 * scale).toFloat()
            val height = (boundingBox.y() * scale).toFloat()
            setWidth(width)
            setHeight(height)
        }
    }

    companion object {
        private val hooked by unsafeLazy {
            Bukkit.getPluginManager().getPlugin("BetterModel") != null
        }

        fun isBetterModelHooked(): Boolean = hooked

        @Awake(LifeCycle.LOAD)
        fun init() {
            Adyeshach.api().getEntityTypeRegistry()
                .prepareGenerate(object : AdyeshachEntityTypeRegistry.GenerateCallback {
                    override fun invoke(entityType: EntityTypes, interfaces: List<String>): List<String> {
                        val array = ArrayList<String>()
                        if (isBetterModelHooked() && entityType == EntityTypes.INTERACTION) {
                            array += DefaultBetterModel::class.java.name
                        }
                        return array
                    }
                })
        }
    }
}
