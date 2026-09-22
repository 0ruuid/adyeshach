package ink.ptms.adyeshach.core.util

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit as submitPlatform
import taboolib.platform.Folia
import taboolib.platform.util.runTask
import taboolib.platform.util.submit as submitAtOwner

/**
 * Centralizes the Paper/Folia execution model.
 *
 * Business code should express which Bukkit resource owns an operation instead
 * of calling a Folia scheduler directly:
 *
 * - NPC/world state -> [runNpc] or [runLocation]
 * - player state -> [runPlayer]
 * - CPU/IO-only work -> the regular async executor
 */
object FoliaRuntime {

    /** Whether the server uses Folia's regionized threading model. */
    val isFolia: Boolean
        get() = Folia.isFolia

    /** Folia cannot synchronously wait for region-owned world access. */
    fun shouldPathfindAsync(pathfinderSync: Boolean): Boolean {
        return isFolia || !pathfinderSync
    }

    /** Executes NPC work in the scheduler that owns the NPC's current region. */
    fun runNpc(entity: EntityInstance, action: () -> Unit) {
        runLocation(entity.getLocation(), action)
    }

    /** Executes delayed NPC work in the scheduler that owns the NPC's current region. */
    fun runNpc(entity: EntityInstance, delay: Long, action: () -> Unit) {
        runLocation(entity.getLocation(), delay, action)
    }

    /** Executes player work in the player's entity scheduler on Folia. */
    fun runPlayer(player: Player, action: () -> Unit) {
        if (isFolia) {
            player.runTask(Runnable(action))
        } else {
            action()
        }
    }

    /**
     * Always schedules player work onto its owning Bukkit thread. This is used
     * when the caller may be a future/network callback rather than a tick task.
     */
    fun schedulePlayer(player: Player, action: () -> Unit) {
        if (isFolia) {
            player.runTask(Runnable(action))
        } else {
            submitPlatform { action() }
        }
    }

    /** Executes delayed player work in the player's entity scheduler on Folia. */
    fun runPlayer(player: Player, delay: Long, action: () -> Unit) {
        if (isFolia) {
            player.submitAtOwner(delay = delay) { action() }
        } else {
            submitPlatform(delay = delay) { action() }
        }
    }

    /** Executes world work in the scheduler that owns the location on Folia. */
    fun runLocation(location: Location, action: () -> Unit) {
        if (isFolia) {
            location.runTask(Runnable(action))
        } else {
            action()
        }
    }

    /** Executes delayed world work in the scheduler that owns the location on Folia. */
    fun runLocation(location: Location, delay: Long, action: () -> Unit) {
        if (isFolia) {
            location.submitAtOwner(delay = delay) { action() }
        } else {
            submitPlatform(delay = delay) { action() }
        }
    }

    /**
     * The caller already owns the required Folia resource; Paper still needs
     * its traditional main-thread dispatch.
     */
    fun runOnPaperMainOrCurrent(action: () -> Unit) {
        if (isFolia) {
            action()
        } else {
            submitPlatform { action() }
        }
    }
}

fun EntityInstance.runOnRegion(action: () -> Unit) {
    FoliaRuntime.runNpc(this, action)
}

fun EntityInstance.runOnRegion(delay: Long, action: () -> Unit) {
    FoliaRuntime.runNpc(this, delay, action)
}

fun Player.runOnEntity(action: () -> Unit) {
    FoliaRuntime.runPlayer(this, action)
}

fun Player.scheduleOnEntity(action: () -> Unit) {
    FoliaRuntime.schedulePlayer(this, action)
}

fun Player.runOnEntity(delay: Long, action: () -> Unit) {
    FoliaRuntime.runPlayer(this, delay, action)
}

fun Location.runOnRegion(action: () -> Unit) {
    FoliaRuntime.runLocation(this, action)
}

fun Location.runOnRegion(delay: Long, action: () -> Unit) {
    FoliaRuntime.runLocation(this, delay, action)
}
