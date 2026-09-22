package ink.ptms.adyeshach.impl.manager

import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.AdyeshachSettings
import ink.ptms.adyeshach.core.event.AdyeshachPlayerJoinEvent
import ink.ptms.adyeshach.core.SpawnTrigger
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerTeleportEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.platform.Folia
import taboolib.platform.util.bukkitPlugin
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.submit as submitForEntity
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Adyeshach
 * ink.ptms.adyeshach.impl.manager.DefaultPlayerEvents
 *
 * @author 坏黑
 * @since 2022/8/18 10:41
 */
internal object DefaultPlayerEvents {

    val onlinePlayerSet = CopyOnWriteArraySet<String>()

    @Awake(LifeCycle.ACTIVE)
    fun onActive() {
        if (Folia.isFolia) {
            return
        }
        // PacketEvents 监听器由 PacketLoader 在 ENABLE 时注册
        // 释放玩家的数据包缓冲区
        val packetHandler = Adyeshach.api().getMinecraftAPI().getPacketHandler()
        Bukkit.getScheduler().runTaskTimerAsynchronously(bukkitPlugin, Runnable {
            onlinePlayers.forEach {
                packetHandler.flush(it)
            }
        }, 1, 1)
    }

    /**
     * 进入游戏初始化管理器
     */
    @SubscribeEvent
    fun onJoin(e: PlayerJoinEvent) {
        if (AdyeshachSettings.spawnTrigger == SpawnTrigger.JOIN) {
            // 延迟初始化
            if (Folia.isFolia) {
                e.player.submitForEntity(delay = AdyeshachSettings.spawnDelay.toLong()) {
                    Adyeshach.api().setupEntityManager(e.player)
                }
            } else {
                submit(delay = AdyeshachSettings.spawnDelay.toLong()) {
                    Adyeshach.api().setupEntityManager(e.player)
                }
            }
        }
    }

    /**
     * 进入游戏初始化管理器（延迟）
     */
    @SubscribeEvent
    fun onLateJoin(e: AdyeshachPlayerJoinEvent) {
        if (AdyeshachSettings.spawnTrigger == SpawnTrigger.KEEP_ALIVE) {
            if (Folia.isFolia) {
                e.player.submitForEntity { Adyeshach.api().setupEntityManager(e.player) }
            } else {
                Adyeshach.api().setupEntityManager(e.player)
            }
        }
    }

    /**
     * 离开游戏释放管理器
     */
    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        onlinePlayerSet -= e.player.name
        Adyeshach.api().getMinecraftAPI().getPacketHandler().cleanup(e.player)
        Adyeshach.api().releaseEntityManager(e.player)
    }

    /**
     * 传送时更新管理器
     */
    @SubscribeEvent(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(e: PlayerTeleportEvent) {
        if (e.from.world == e.to.world && e.from.distance(e.to) > AdyeshachSettings.visibleDistance) {
            if (Folia.isFolia) {
                e.player.submitForEntity(delay = 20) { Adyeshach.api().refreshEntityManager(e.player) }
            } else {
                submit(delay = 20) { Adyeshach.api().refreshEntityManager(e.player) }
            }
        }
    }

    /**
     * 切换世界时更新管理器
     */
    @SubscribeEvent(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(e: PlayerChangedWorldEvent) {
        if (Folia.isFolia) {
            e.player.submitForEntity(delay = 20) { Adyeshach.api().refreshEntityManager(e.player) }
        } else {
            submit(delay = 20) { Adyeshach.api().refreshEntityManager(e.player) }
        }
    }

}
