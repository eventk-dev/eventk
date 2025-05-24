package dev.eventk.store.test.w.driver

import kotlinx.serialization.Serializable

@Serializable
public sealed interface DriverEvent

@Serializable
public data class DriverRegisteredEvent(
    val licence: String,
    val name: String,
) : DriverEvent

@Serializable
public data class DriverUnregisteredEvent(
    val licence: String,
) : DriverEvent
