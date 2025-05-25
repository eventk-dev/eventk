package dev.eventk.store.impl.pg

import dev.eventk.store.api.EventMetadata
import dev.eventk.store.api.Serializer
import dev.eventk.store.api.StreamType
import dev.eventk.store.impl.common.string.serialization.DefaultEventMetadataSerializer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.UUID
import kotlin.reflect.KClass

internal class PostgresqlConfig<DS>(
    val registeredTypes: List<StreamType<*, *>>,
    internal val payloadSerializers: Map<StreamType<*, *>, Serializer<*, String>>,
    internal val idSerializers: Map<StreamType<*, *>, Serializer<*, String>>,
    internal val eventMetadataSerializer: Serializer<EventMetadata, String>,
    val dataSource: DS,
    val eventTable: String,
    val eventWriteTable: String = eventTable,
) {
    private val registeredTypeById: Map<String, StreamType<*, *>> by lazy { registeredTypes.associateBy { it.id } }

    @Suppress("UNCHECKED_CAST")
    fun <E, I, S : StreamType<E, I>> streamType(id: String): S {
        return registeredTypeById[id] as S? ?: throw IllegalStateException("Invalid stream type id $id")
    }

    val tableInfo = TableInfo(
        table = eventTable,
        writeTable = eventWriteTable,
    )
}

internal data class ConnectionConfig(
    val host: String,
    val port: Int,
    val database: String,
    val schema: String,
    val user: String,
    val pass: String,
    val minPoolSize: Int = 1,
    val maxPoolSize: Int = 2,
)

internal data class TableInfo(
    val table: String,
    val writeTable: String,
)

public class PostgresqlConfigBuilder<DS>(
    private val datasource: DS,
    private val eventTable: String,
    /**
     * In case you need your event table to be a view, you can define a different table name that will be used for write operations only.
     */
    private val eventWriteTable: String = eventTable,
) {
    private val registeredTypes = mutableListOf<StreamType<*, *>>()
    private val payloadSerializers = mutableMapOf<StreamType<*, *>, Serializer<*, String>>()
    private val idSerializers = mutableMapOf<StreamType<*, *>, Serializer<*, String>>()
    private var eventMetadataSerializer: Serializer<EventMetadata, String> = DefaultEventMetadataSerializer

    public var serialFormat: StringFormat = Json.Default

    public fun <E, I, T> registerStreamTypeWith(streamType: T, payloadSerializer: Serializer<E, String>, idSerializer: Serializer<I, String>)
    where T : StreamType<E, I> {
        registeredTypes += streamType as StreamType<*, *>
        payloadSerializers[streamType] = payloadSerializer as Serializer<*, String>
        idSerializers[streamType] = idSerializer as Serializer<*, String>
    }

    public inline fun <reified E : Any, reified I : Any, T> registerStreamType(
        streamType: T,
        payloadSerializer: Serializer<E, String> = createDefaultPayloadSerializer(E::class),
        idSerializer: Serializer<I, String> = createDefaultIdSerializer(I::class),
    )
    where T : StreamType<E, I> {
        registerStreamTypeWith(streamType, payloadSerializer, idSerializer)
    }

    public fun eventMetadataSerializer(eventMetadataSerializer: Serializer<EventMetadata, String>) {
        this.eventMetadataSerializer = eventMetadataSerializer
    }

    @OptIn(InternalSerializationApi::class)
    public fun <E : Any> createDefaultPayloadSerializer(type: KClass<E>): Serializer<E, String> {
        return KotlinxSerializationSerializableAdapter(
            try {
                type.serializer()
            } catch (e: SerializationException) {
                throw IllegalStateException("$type is not marked with @Serializable, please register this type with an explicit serializer", e)
            },
        )
    }

    private inner class KotlinxSerializationSerializableAdapter<E>(private val serializer: KSerializer<E>) : Serializer<E, String> {
        override fun serialize(obj: E): String = serialFormat.encodeToString(serializer, obj)
        override fun deserialize(payload: String): E = serialFormat.decodeFromString(serializer, payload)
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(InternalSerializationApi::class)
    public fun <I : Any> createDefaultIdSerializer(type: KClass<I>): Serializer<I, String> {
        return when (type) {
            String::class -> object : Serializer<I, String> {
                override fun serialize(obj: I): String = obj as String
                override fun deserialize(payload: String): I = payload as I
            }
            UUID::class -> object : Serializer<I, String> {
                override fun serialize(obj: I): String = obj.toString()
                override fun deserialize(payload: String): I = UUID.fromString(payload) as I
            }
            else -> throw IllegalStateException(
                "$type cannot be serialized automatically, please register this type with an explicit id serializer",
            )
        }
    }

    internal fun build(): PostgresqlConfig<DS> = PostgresqlConfig(
        registeredTypes = registeredTypes,
        payloadSerializers = payloadSerializers,
        idSerializers = idSerializers,
        eventMetadataSerializer = eventMetadataSerializer,
        dataSource = datasource,
        eventTable = eventTable,
        eventWriteTable = eventWriteTable,
    )
}
