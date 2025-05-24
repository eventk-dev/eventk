package dev.eventk.store.test

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.EventMetadata
import dev.eventk.store.api.StreamType
import dev.eventk.store.storage.api.blocking.Storage

internal fun <E, I> Storage.add(streamType: StreamType<E, I>, streamId: I, version: Int, event: E, metadata: EventMetadata = emptyMap()) {
    add(streamType, streamId, version - 1, listOf(event), metadata)
}

internal fun <E, I> Storage.getEventByPosition(position: Long): EventEnvelope<E, I> {
    return loadEventBatch<E, I>(position - 1, 1).single()
}

internal fun <E, I> Storage.getEventByStreamVersion(streamType: StreamType<E, I>, streamId: I, version: Int): EventEnvelope<E, I> {
    return useStreamEvents(streamType, streamId, version - 1) { stream ->
        stream.first()
    }
}
