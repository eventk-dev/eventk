package dev.eventk.store.impl.fs

import dev.eventk.store.api.EventMetadata
import dev.eventk.store.api.Serializer
import dev.eventk.store.api.StreamType
import dev.eventk.store.impl.common.binary.serialization.DefaultEventMetadataSerializer
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import okio.Path
import okio.Path.Companion.toPath
import kotlin.reflect.KClass

internal class FileSystemConfig(
    val basePath: Path,
    val registeredTypes: List<StreamType<*, *>>,
    internal val payloadSerializers: Map<StreamType<*, *>, Serializer<*, ByteArray>>,
    internal val idSerializers: Map<StreamType<*, *>, Serializer<*, String>>,
    internal val eventMetadataSerializer: Serializer<EventMetadata, ByteArray>,
)

public class FileSystemConfigBuilder(
    private val basePath: String,
) {
    private val registeredTypes = mutableListOf<StreamType<*, *>>()
    private val payloadSerializers = mutableMapOf<StreamType<*, *>, Serializer<*, ByteArray>>()
    private val idSerializers = mutableMapOf<StreamType<*, *>, Serializer<*, String>>()
    private var eventMetadataSerializer: Serializer<EventMetadata, ByteArray> = DefaultEventMetadataSerializer

    @OptIn(ExperimentalSerializationApi::class)
    public var serialFormat: BinaryFormat = ProtoBuf.Default

    public fun <E, I, T> registerStreamTypeWith(streamType: T, payloadSerializer: Serializer<E, ByteArray>, idSerializer: Serializer<I, String>)
    where T : StreamType<E, I> {
        registeredTypes += streamType as StreamType<*, *>
        payloadSerializers[streamType] = payloadSerializer as Serializer<*, ByteArray>
        idSerializers[streamType] = idSerializer as Serializer<*, String>
    }

    public inline fun <reified E : Any, reified I : Any, T> registerStreamType(
        streamType: T,
        payloadSerializer: Serializer<E, ByteArray> = createDefaultPayloadSerializer(E::class),
        idSerializer: Serializer<I, String> = createDefaultIdSerializer(I::class),
    )
    where T : StreamType<E, I> {
        registerStreamTypeWith(streamType, payloadSerializer, idSerializer)
    }

    public fun eventMetadataSerializer(eventMetadataSerializer: Serializer<EventMetadata, ByteArray>) {
        this.eventMetadataSerializer = eventMetadataSerializer
    }

    @OptIn(InternalSerializationApi::class)
    public fun <E : Any> createDefaultPayloadSerializer(type: KClass<E>): Serializer<E, ByteArray> {
        return KotlinxSerializationSerializableAdapter(
            try {
                type.serializer()
            } catch (e: SerializationException) {
                throw IllegalStateException("$type is not marked with @Serializable, please register this type with an explicit serializer", e)
            },
        )
    }

    private inner class KotlinxSerializationSerializableAdapter<E>(private val serializer: KSerializer<E>) : Serializer<E, ByteArray> {
        override fun serialize(obj: E): ByteArray {
            return serialFormat.encodeToByteArray(serializer, obj)
        }

        override fun deserialize(payload: ByteArray): E {
            return serialFormat.decodeFromByteArray(serializer, payload)
        }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <I : Any> createDefaultIdSerializer(type: KClass<I>): Serializer<I, String> {
        return when (type) {
            String::class -> object : Serializer<I, String> {
                override fun serialize(obj: I): String = obj as String
                override fun deserialize(payload: String): I = payload as I
            }
            else -> throw IllegalStateException(
                "$type cannot be serialized automatically, please register this type with an explicit id serializer",
            )
        }
    }

    internal fun build(): FileSystemConfig = FileSystemConfig(
        basePath = basePath.toPath(true),
        registeredTypes = registeredTypes,
        payloadSerializers = payloadSerializers,
        idSerializers = idSerializers,
        eventMetadataSerializer = eventMetadataSerializer,
    )
}
