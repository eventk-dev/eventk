package dev.eventk.store.api

public data class StreamVersionMismatchException(val currentVersion: Int, val expectedVersion: Int) : RuntimeException()
