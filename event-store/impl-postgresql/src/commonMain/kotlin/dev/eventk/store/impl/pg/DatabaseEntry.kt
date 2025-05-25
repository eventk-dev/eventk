package dev.eventk.store.impl.pg

internal class DatabaseEntry(
    val position: Long = -1,
    val type: String,
    val id: String,
    val version: Int,
    val eventPayload: String,
    val metadataPayload: String,
)
