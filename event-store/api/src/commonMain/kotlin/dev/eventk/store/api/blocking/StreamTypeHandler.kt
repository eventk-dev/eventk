package dev.eventk.store.api.blocking

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.EventMetadata
import dev.eventk.store.api.StreamType
import dev.eventk.store.api.StreamVersionMismatchException

public interface StreamTypeHandler<E, I> {
    public val streamType: StreamType<E, I>

    /**
     * Load events from an event stream.
     */
    public fun loadStream(
        streamId: I,
        sinceVersion: Int = 0,
    ): List<EventEnvelope<E, I>>

    /**
     * Allow events from the stream to be consumed through a [Sequence] in a safe way.
     * Events from the stream will be loaded progressively as the sequence is iterated on,
     * and all underlying resources will be closed by the end of this call.
     */
    public fun <R> useStream(
        streamId: I,
        sinceVersion: Int = 0,
        consume: (Sequence<EventEnvelope<E, I>>) -> R,
    ): R

    /**
     * Append new events into an event stream.
     *
     * @return the version of the aggregate after those events are appended, or;
     * @throws [StreamVersionMismatchException] when the stream currently is not in the expected version.
     */
    public fun appendStream(
        streamId: I,
        expectedVersion: Int,
        events: List<E>,
        metadata: EventMetadata = emptyMap(),
    ): Int

    /**
     * Load events from a stream and optionally append new ones atomically, under a per-stream lock held for the
     * duration of [block].
     *
     * [block] receives the currently-stored envelopes (from [sinceVersion]) and an [appendStream] function it may
     * invoke at most once to atomically append new events to the same stream. The function returns the freshly
     * appended envelopes (with assigned versions and positions). Calling [appendStream] more than once throws
     * [IllegalStateException]; not calling it is fine and commits the lock release without writing anything.
     *
     * If [block] throws, nothing is appended.
     */
    public fun <R> loadStreamForAppend(
        streamId: I,
        sinceVersion: Int = 0,
        block: (
            loaded: List<EventEnvelope<E, I>>,
            appendStream: (events: List<E>, metadata: EventMetadata) -> List<EventEnvelope<E, I>>,
        ) -> R,
    ): R
}
