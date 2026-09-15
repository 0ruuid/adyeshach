package ink.ptms.adyeshach.compat.bettermodel

import ink.ptms.adyeshach.core.entity.BetterModelView
import ink.ptms.adyeshach.core.entity.EntityInstance
import ink.ptms.adyeshach.core.util.errorBy
import ink.ptms.adyeshach.impl.getEntities
import ink.ptms.adyeshach.impl.getManager
import ink.ptms.adyeshach.impl.isEntitySelected
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter
import kr.toxicity.model.api.tracker.DummyTracker
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.util.isPlayer
import taboolib.module.kether.*

@Awake(LifeCycle.LOAD)
internal fun init() {
    if (DefaultBetterModel.isBetterModelHooked()) {
        KetherLoader.registerParser(combinationParser {
            it.group(
                symbol(),
                symbol(),
                text(),
                command("speed", then = double()).option().defaultsTo(1.0),
                command("lerpin", then = int()).option().defaultsTo(0),
                command("lerpout", then = int()).option().defaultsTo(0),
                command("type", then = text()).option().defaultsTo("PLAY_ONCE"),
            ).apply(it) { action, method, animationName, speed, lerpin, lerpout, typeStr ->
                now {
                    if (script().getManager() == null || !script().isEntitySelected()) {
                        errorBy("error-no-manager-or-entity-selected")
                    }
                    val isAllPlayers = action.lowercase() == "animation-all"
                    val sender = if (!isAllPlayers && script().sender?.isPlayer() == true) {
                        script().sender!!.cast<Player>()
                    } else {
                        null
                    }
                    val animationType = try {
                        AnimationIterator.Type.valueOf(typeStr.uppercase())
                    } catch (e: IllegalArgumentException) {
                        AnimationIterator.Type.PLAY_ONCE
                    }
                    script().getEntities().filterIsInstance<BetterModelView>().forEach { entity ->
                        val entityInstance = entity as EntityInstance
                        val dummyTracker = entityInstance.getTag("BetterModel:DummyTracker") as? DummyTracker
                        if (dummyTracker != null) {
                            when (method.lowercase()) {
                                "add" -> {
                                    val modifier = AnimationModifier.builder()
                                        .start(lerpin)
                                        .end(lerpout)
                                        .type(animationType)
                                        .speed(speed.toFloat())
                                        .apply {
                                            if (!isAllPlayers && sender != null) {
                                                player(BukkitAdapter.adapt(sender))
                                            }
                                        }
                                        .build()
                                    dummyTracker.animate(animationName, modifier)
                                }
                                "remove" -> {
                                    if (isAllPlayers) {
                                        dummyTracker.stopAnimation(animationName)
                                    } else {
                                        if (sender != null) {
                                            dummyTracker.stopAnimation({ true }, animationName, BukkitAdapter.adapt(sender))
                                        } else {
                                            dummyTracker.stopAnimation(animationName)
                                        }
                                    }
                                }
                                else -> error("Unknown method: $method (add, remove)")
                            }
                        }
                    }
                }
            }
        }, arrayOf("bettermodel"), "adyeshach", true)
    }
}
