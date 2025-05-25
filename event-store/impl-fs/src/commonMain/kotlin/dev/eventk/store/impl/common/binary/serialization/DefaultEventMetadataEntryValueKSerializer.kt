package dev.eventk.store.impl.common.binary.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
internal object DefaultEventMetadataEntryValueKSerializer : KSerializer<Any> {
    private const val STRING_HEADER: Short = 0
    private const val BOOLEAN_HEADER: Short = 1
    private const val INT_HEADER: Short = 2
    private const val LONG_HEADER: Short = 3
    private const val FLOAT_HEADER: Short = 4
    private const val DOUBLE_HEADER: Short = 5

    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("EventMetadataEntryValueSerializer", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: Any) {
        when (value) {
            is String -> {
                encoder.encodeShort(STRING_HEADER)
                encoder.encodeString(value)
            }

            is Boolean -> {
                encoder.encodeShort(BOOLEAN_HEADER)
                encoder.encodeBoolean(value)
            }

            is Int -> {
                encoder.encodeShort(INT_HEADER)
                encoder.encodeInt(value)
            }

            is Long -> {
                encoder.encodeShort(LONG_HEADER)
                encoder.encodeLong(value)
            }

            is Float -> {
                encoder.encodeShort(FLOAT_HEADER)
                encoder.encodeFloat(value)
            }

            is Double -> {
                encoder.encodeShort(DOUBLE_HEADER)
                encoder.encodeDouble(value)
            }
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        return when (val header = decoder.decodeShort()) {
            STRING_HEADER -> decoder.decodeString()
            BOOLEAN_HEADER -> decoder.decodeBoolean()
            INT_HEADER -> decoder.decodeInt()
            LONG_HEADER -> decoder.decodeLong()
            FLOAT_HEADER -> decoder.decodeFloat()
            DOUBLE_HEADER -> decoder.decodeDouble()
            else -> throw IllegalStateException("unsupported header: $header")
        }
    }
}
