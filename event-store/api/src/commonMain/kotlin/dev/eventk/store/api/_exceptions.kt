package dev.eventk.store.api

public data class StreamVersionMismatchException(val currentVersion: Int, val expectedVersion: Int) : RuntimeException()

/** Unchecked wrapper for a failure raised while holding or acquiring the per-stream append lock. */
public class EventStreamLockException(cause: Throwable) : RuntimeException(cause.message, cause)
