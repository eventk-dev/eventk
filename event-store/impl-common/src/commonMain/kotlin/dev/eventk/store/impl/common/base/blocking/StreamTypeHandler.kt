package dev.eventk.store.impl.common.base.blocking

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.EventMetadata
import dev.eventk.store.api.StreamType
import dev.eventk.store.api.StreamVersionMismatchException
import dev.eventk.store.storage.api.StorageVersionMismatchException
import dev.eventk.store.storage.api.blocking.Storage

public class StreamTypeHandler<E, I>(
    override val streamType: StreamType<E, I>,
    private val storage: Storage,
) : dev.eventk.store.api.blocking.StreamTypeHandler<E, I> {
    override fun loadStream(streamId: I, sinceVersion: Int): List<EventEnvelope<E, I>> {
        return storage.useStreamEvents(streamType, streamId, sinceVersion) { stream ->
            stream.toList()
        }
    }

    override fun <R> useStream(streamId: I, sinceVersion: Int, consume: (Sequence<EventEnvelope<E, I>>) -> R): R {
        return storage.useStreamEvents(streamType, streamId, sinceVersion, consume)
    }

    override fun appendStream(streamId: I, expectedVersion: Int, events: List<E>, metadata: EventMetadata): Int {
        try {
            storage.add(streamType, streamId, expectedVersion, events, metadata)
        } catch (e: StorageVersionMismatchException) {
            throw StreamVersionMismatchException(e.currentVersion, e.expectedVersion)
        }
        return expectedVersion + events.size
    }
}
