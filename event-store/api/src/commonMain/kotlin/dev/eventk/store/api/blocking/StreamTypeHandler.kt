package dev.eventk.store.api.blocking

import dev.eventk.store.api.AppendResult
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
     * Load events from a stream and append new ones atomically, under a per-stream lock acquired before the load.
     *
     * The [consume] lambda receives a lazy [Sequence] of currently-stored envelopes and returns the events to append
     * (as an [AppendResult]). Whatever envelopes the lambda iterates through the sequence are captured and passed
     * to [finalize] alongside the freshly-appended envelopes (with their assigned versions and positions).
     *
     * If [consume] returns an empty [AppendResult.events], no rows are written but the operation is still committed.
     * If [consume] throws, nothing is appended.
     */
    public fun <R> loadAndAppendStream(
        streamId: I,
        sinceVersion: Int = 0,
        consume: (List<EventEnvelope<E, I>>) -> AppendResult<E>,
        finalize: (loaded: List<EventEnvelope<E, I>>, appended: List<EventEnvelope<E, I>>) -> R,
    ): R

    public fun loadAndAppendStream(
        streamId: I,
        sinceVersion: Int = 0,
        consume: (List<EventEnvelope<E, I>>) -> AppendResult<E>,
    ): Unit = loadAndAppendStream(streamId, sinceVersion, consume, finalize = { _, _ -> })
}
