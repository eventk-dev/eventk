package dev.eventk.store.impl.common.binary.serialization

import dev.eventk.store.api.EventMetadata
import dev.eventk.store.api.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

// TODO move this to a `impl-common-binary` module when there is another binary implementation

/**
 * Default binary serializer for [EventMetadata].
 */
@OptIn(ExperimentalSerializationApi::class)
public object DefaultEventMetadataSerializer : Serializer<EventMetadata, ByteArray> {
    private val cbor = Cbor {
        serializersModule = SerializersModule {
            contextual(Any::class, DefaultEventMetadataEntryValueKSerializer)
        }
    }

    private val serializer = cbor.serializersModule.serializer<EventMetadata>()

    override fun serialize(obj: EventMetadata): ByteArray {
        return cbor.encodeToByteArray(serializer, obj)
    }

    override fun deserialize(payload: ByteArray): EventMetadata {
        return cbor.decodeFromByteArray(serializer, payload)
    }
}
