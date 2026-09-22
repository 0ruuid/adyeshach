package ink.ptms.adyeshach.impl.pe

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.MinecraftMeta
import ink.ptms.adyeshach.core.MinecraftPacketHandler
import org.bukkit.entity.Player
import ink.ptms.adyeshach.core.util.FoliaRuntime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * PacketEvents 实现的 MinecraftPacketHandler
 */
class PEPacketHandler : MinecraftPacketHandler {

    private val buffer = ConcurrentHashMap<Player, ConcurrentLinkedQueue<Any>>()
    private val metaBuffer = ConcurrentHashMap<Player, ConcurrentLinkedQueue<BufferPacket>>()

    override fun sendPacket(player: List<Player>, packet: Any) {
        if (FoliaRuntime.isFolia) {
            player.forEach { sendNow(it, packet) }
            return
        }
        player.forEach {
            buffer.getOrPut(it) { ConcurrentLinkedQueue() }.offer(packet)
        }
    }

    override fun bufferMetadataPacket(player: List<Player>, id: Int, packet: MinecraftMeta) {
        if (FoliaRuntime.isFolia) {
            val metadataHandler = Adyeshach.api().getMinecraftAPI().getEntityMetadataHandler()
            player.forEach { sendNow(it, metadataHandler.createMetadataPacket(id, listOf(packet))) }
            return
        }
        player.forEach {
            metaBuffer.getOrPut(it) { ConcurrentLinkedQueue() }.offer(BufferPacket(id, packet))
        }
    }

    override fun flush(player: List<Player>) {
        player.forEach { p ->
            val user = PacketEvents.getAPI().playerManager.getUser(p) ?: return@forEach
            drain(buffer[p]).forEach { packet ->
                when (packet) {
                    is PacketWrapper<*> -> PacketEvents.getAPI().protocolManager.sendPacket(user.channel, packet)
                    else -> PacketEvents.getAPI().protocolManager.sendPacket(user.channel, packet)
                }
            }
            val metadata = drain(metaBuffer[p])
            if (metadata.isNotEmpty()) {
                val metadataHandler = Adyeshach.api().getMinecraftAPI().getEntityMetadataHandler()
                val packets = metadata.groupBy { it.id }.map { (id, packets) ->
                    metadataHandler.createMetadataPacket(id, packets.map { it.packet })
                }
                packets.forEach { packet ->
                    when (packet) {
                        is PacketWrapper<*> -> PacketEvents.getAPI().protocolManager.sendPacket(user.channel, packet)
                        else -> PacketEvents.getAPI().protocolManager.sendPacket(user.channel, packet)
                    }
                }
            }
        }
    }

    private fun sendNow(player: Player, packet: Any) {
        val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
        when (packet) {
            is PacketWrapper<*> -> PacketEvents.getAPI().protocolManager.sendPacket(user.channel, packet)
            else -> PacketEvents.getAPI().protocolManager.sendPacket(user.channel, packet)
        }
    }

    override fun cleanup(player: Player) {
        buffer.remove(player)
        metaBuffer.remove(player)
    }

    private fun <T> drain(queue: ConcurrentLinkedQueue<T>?): List<T> {
        if (queue == null) return emptyList()
        val packets = ArrayList<T>()
        repeat(MAX_DRAIN_SIZE) {
            val packet = queue.poll() ?: return packets
            packets += packet
        }
        return packets
    }

    private data class BufferPacket(val id: Int, val packet: MinecraftMeta)

    private companion object {
        const val MAX_DRAIN_SIZE = 4096
    }
}
