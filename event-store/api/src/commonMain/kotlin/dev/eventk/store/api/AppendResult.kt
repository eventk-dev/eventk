package dev.eventk.store.api

public data class AppendResult<out E>(
    val events: List<E>,
    val metadata: EventMetadata = emptyMap(),
)
