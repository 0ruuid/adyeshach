package ink.ptms.adyeshach.impl.util

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit as submitPlatform
import taboolib.platform.Folia
import taboolib.platform.util.runTask
import taboolib.platform.util.submit as submitAtLocation

/** Runs Bukkit-facing NPC work in the region that owns the NPC location. */
fun EntityInstance.runOnRegion(action: () -> Unit) {
    getLocation().runOnRegion(action)
}

/** Runs delayed Bukkit-facing NPC work in the region that owns the NPC location. */
fun EntityInstance.runOnRegion(delay: Long, action: () -> Unit) {
    getLocation().runOnRegion(delay, action)
}

/** Runs Bukkit-facing player work in the player's entity scheduler on Folia. */
fun Player.runOnEntity(action: () -> Unit) {
    if (Folia.isFolia) {
        runTask(Runnable { action() })
    } else {
        action()
    }
}

/** Runs delayed Bukkit-facing player work in the player's entity scheduler on Folia. */
fun Player.runOnEntity(delay: Long, action: () -> Unit) {
    if (Folia.isFolia) {
        submitAtLocation(delay = delay) { action() }
    } else {
        submitPlatform(delay = delay) { action() }
    }
}

/** Runs Bukkit-facing world work in the region that owns this location. */
fun Location.runOnRegion(action: () -> Unit) {
    if (Folia.isFolia) {
        runTask(Runnable { action() })
    } else {
        action()
    }
}

/** Runs delayed Bukkit-facing world work in the region that owns this location. */
fun Location.runOnRegion(delay: Long, action: () -> Unit) {
    if (Folia.isFolia) {
        submitAtLocation(delay = delay) { action() }
    } else {
        submitPlatform(delay = delay) { action() }
    }
}
