package dev.eventk.store.test.w.part

import kotlinx.serialization.Serializable

@Serializable
public sealed interface PartEvent

@Serializable
public data class PartProducedEvent(
    val producer: Long,
    val partNumber: String,
) : PartEvent
