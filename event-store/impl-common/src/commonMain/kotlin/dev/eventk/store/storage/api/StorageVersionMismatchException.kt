package dev.eventk.store.storage.api

public data class StorageVersionMismatchException(
    val currentVersion: Int,
    val expectedVersion: Int,
) : Exception("Expected version is $expectedVersion, but current version is $currentVersion")
