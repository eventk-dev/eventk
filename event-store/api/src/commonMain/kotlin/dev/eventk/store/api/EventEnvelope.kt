package dev.eventk.store.api

public data class EventEnvelope<out E, I>(
    val streamType: StreamType<@UnsafeVariance E, I>,
    val streamId: I,
    val version: Int,
    val position: Long,
    val metadata: EventMetadata,
    val event: E,
)
