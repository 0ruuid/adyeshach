package ink.ptms.adyeshach.module.editor.meta.impl

import ink.ptms.adyeshach.core.entity.EntityInstance
import ink.ptms.adyeshach.core.util.Components
import ink.ptms.adyeshach.core.util.scheduleOnEntity
import ink.ptms.adyeshach.core.util.sendLang
import ink.ptms.adyeshach.module.editor.clearScreen
import ink.ptms.adyeshach.module.editor.meta.MetaEditor
import org.bukkit.entity.Player
import taboolib.platform.util.nextChat

/**
 * Adyeshach
 * ink.ptms.adyeshach.module.editor.meta.impl.MetaText
 *
 * @author 坏黑
 * @since 2022/12/27 04:04
 */
class MetaText(val key: String) : MetaEditor {

    override fun open(entity: EntityInstance, player: Player, def: String) {
        val plainMessage = Components.toLegacyText(def)
        player.clearScreen()
        player.sendLang("editor-input-chat-component", plainMessage, plainMessage.replace('§', '&'), def.replace('§', '&'))
        player.nextChat {
            player.scheduleOnEntity { player.chat("/adyeshach api ee adyeshach edit ${entity.uniqueId} m:$key->${it}") }
        }
    }
}