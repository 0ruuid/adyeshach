package ink.ptms.adyeshach.impl.pe

import ink.ptms.adyeshach.core.MinecraftMeta
import ink.ptms.adyeshach.core.MinecraftMetadataParser
import ink.ptms.adyeshach.core.bukkit.BukkitParticles
import ink.ptms.adyeshach.core.bukkit.BukkitPose
import ink.ptms.adyeshach.core.bukkit.data.VillagerData
import ink.ptms.adyeshach.core.entity.type.AdySniffer
import org.bukkit.Art
import taboolib.module.chat.ComponentText
import org.bukkit.inventory.ItemStack
import org.bukkit.material.MaterialData
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import taboolib.common5.Coerce
import taboolib.common5.Quat
import java.util.UUID

@Suppress("UNCHECKED_CAST")
object PEParsers {

    class ByteParser : MinecraftMetadataParser<Byte>() {
        override fun parse(value: Any) = Coerce.toByte(value)
        override fun createMeta(index: Int, value: Byte) = metadataHandler().createByteMeta(index, value)
    }

    class IntParser : MinecraftMetadataParser<Int>() {
        override fun parse(value: Any) = Coerce.toInteger(value)
        override fun createMeta(index: Int, value: Int) = metadataHandler().createIntMeta(index, value)
    }

    class FloatParser : MinecraftMetadataParser<Float>() {
        override fun parse(value: Any) = Coerce.toFloat(value)
        override fun createMeta(index: Int, value: Float) = metadataHandler().createFloatMeta(index, value)
    }

    class BooleanParser : MinecraftMetadataParser<Boolean>() {
        override fun parse(value: Any) = Coerce.toBoolean(value)
        override fun createMeta(index: Int, value: Boolean) = metadataHandler().createBooleanMeta(index, value)
    }

    class StringParser : MinecraftMetadataParser<String>() {
        override fun parse(value: Any) = value.toString()
        override fun createMeta(index: Int, value: String) = metadataHandler().createStringMeta(index, value)
    }

    class UUIDParser : MinecraftMetadataParser<UUID>() {
        override fun parse(value: Any) = UUID.fromString(value.toString())
        override fun createMeta(index: Int, value: UUID) = metadataHandler().createUUIDMeta(index, value)
    }

    class ChatParser : MinecraftMetadataParser<String>() {
        override fun parse(value: Any): String {
            return when (value) {
                is ComponentText -> value.toLegacyText()
                else -> value.toString()
            }
        }
        override fun createMeta(index: Int, value: String) = metadataHandler().createChatMeta(index, value)
    }

    class OptChatParser : MinecraftMetadataParser<String>() {
        override fun parse(value: Any): String {
            val str = when (value) {
                is ComponentText -> value.toLegacyText()
                else -> value.toString()
            }
            return str.ifBlank { "" }
        }
        override fun createMeta(index: Int, value: String) = metadataHandler().createOptChatMeta(index, value.takeUnless { it.isBlank() })
    }

    class ItemStackParser : MinecraftMetadataParser<ItemStack>() {
        override fun parse(value: Any) = value as ItemStack
        override fun createMeta(index: Int, value: ItemStack) = metadataHandler().createItemStackMeta(index, value)
    }

    class EulerAngleParser : MinecraftMetadataParser<EulerAngle>() {
        override fun parse(value: Any): EulerAngle {
            val v = value as? Vector ?: return EulerAngle(0.0, 0.0, 0.0)
            return EulerAngle(v.x, v.y, v.z)
        }
        override fun createMeta(index: Int, value: EulerAngle) = metadataHandler().createEulerAngleMeta(index, value)
    }

    class VillagerDataParser : MinecraftMetadataParser<VillagerData>() {
        override fun parse(value: Any) = value as VillagerData
        override fun createMeta(index: Int, value: VillagerData) = metadataHandler().createVillagerDataMeta(index, value)
    }

    class BukkitParticleParser : MinecraftMetadataParser<BukkitParticles>() {
        override fun parse(value: Any) = BukkitParticles.valueOf(value.toString())
        override fun createMeta(index: Int, value: BukkitParticles) = metadataHandler().createParticleMeta(index, value)
    }

    class BukkitPoseParser : MinecraftMetadataParser<BukkitPose>() {
        override fun parse(value: Any) = BukkitPose.valueOf(value.toString())
        override fun createMeta(index: Int, value: BukkitPose) = metadataHandler().createPoseMeta(index, value)
    }

    class CatVariantParser : MinecraftMetadataParser<Any>() {
        override fun parse(value: Any) = value
        override fun createMeta(index: Int, value: Any) = metadataHandler().createCatVariantMeta(index, value)
    }

    class FrogVariantParser : MinecraftMetadataParser<Any>() {
        override fun parse(value: Any) = value
        override fun createMeta(index: Int, value: Any) = metadataHandler().createFrogVariantMeta(index, value)
    }

    class PaintingVariantParser : MinecraftMetadataParser<Art>() {
        override fun parse(value: Any) = Art.valueOf(value.toString())
        override fun createMeta(index: Int, value: Art) = metadataHandler().createPaintingVariantMeta(index, value)
    }

    class Vector3Parser : MinecraftMetadataParser<Vector>() {
        override fun parse(value: Any) = value as? Vector ?: Vector(0, 0, 0)
        override fun createMeta(index: Int, value: Vector) = metadataHandler().createVector3Meta(index, value)
    }

    class QuaternionParser : MinecraftMetadataParser<Quat>() {
        override fun parse(value: Any) = value as Quat
        override fun createMeta(index: Int, value: Quat) = metadataHandler().createQuaternionMeta(index, value)
    }

    class SnifferStateParser : MinecraftMetadataParser<AdySniffer.State>() {
        override fun parse(value: Any) = AdySniffer.State.valueOf(value.toString())
        override fun createMeta(index: Int, value: AdySniffer.State) = metadataHandler().createSnifferStateMeta(index, value)
    }

    class BlockStateParser : MinecraftMetadataParser<MaterialData>() {
        override fun parse(value: Any) = value as MaterialData
        override fun createMeta(index: Int, value: MaterialData) = metadataHandler().createBlockStateMeta(index, value)
    }

    class OptBlockStateParser : MinecraftMetadataParser<MaterialData?>() {
        override fun parse(value: Any) = value as? MaterialData
        override fun createMeta(index: Int, value: MaterialData?) = metadataHandler().createOptBlockStateMeta(index, value)
    }

    class OptBlockPosParser : MinecraftMetadataParser<Vector?>() {
        override fun parse(value: Any) = value as? Vector
        override fun createMeta(index: Int, value: Vector?) = metadataHandler().createOptBlockPosMeta(index, value)
    }
}
