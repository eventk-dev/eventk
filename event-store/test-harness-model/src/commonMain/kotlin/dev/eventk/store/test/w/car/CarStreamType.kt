package dev.eventk.store.test.w.car

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuidFrom
import dev.eventk.store.api.Serializer
import dev.eventk.store.api.StreamType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
public data object CarStreamType : StreamType<CarEvent, Uuid> {
    private val eventSerializer = CarEvent.serializer()

    override val id: String = "Car"

    val stringIdSerializer: Serializer<Uuid, String> = object : Serializer<Uuid, String> {
        override fun serialize(obj: Uuid): String {
            return obj.toString()
        }

        override fun deserialize(payload: String): Uuid {
            return uuidFrom(payload)
        }
    }

    val stringEventSerializer: Serializer<CarEvent, String> = object : Serializer<CarEvent, String> {
        private val json = Json {
            ignoreUnknownKeys = true
        }

        override fun serialize(obj: CarEvent): String {
            return json.encodeToString(eventSerializer, obj)
        }

        override fun deserialize(payload: String): CarEvent {
            return json.decodeFromString(eventSerializer, payload)
        }
    }

    val binaryEventSerializer: Serializer<CarEvent, ByteArray> = object : Serializer<CarEvent, ByteArray> {
        private val cbor = Cbor {
            ignoreUnknownKeys = true
        }

        override fun serialize(obj: CarEvent): ByteArray {
            return cbor.encodeToByteArray(eventSerializer, obj)
        }

        override fun deserialize(payload: ByteArray): CarEvent {
            return cbor.decodeFromByteArray(eventSerializer, payload)
        }
    }
}
