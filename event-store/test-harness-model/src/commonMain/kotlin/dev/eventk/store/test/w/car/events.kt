package dev.eventk.store.test.w.car

import kotlinx.serialization.Serializable

@Serializable
public sealed interface CarEvent

@Serializable
public data class CarProducedEvent(
    val vin: String,
    val producer: Long,
    val make: String,
    val model: String,
) : CarEvent

@Serializable
public data class CarSoldEvent(
    val seller: Long,
    val buyer: Long,
    val price: Float,
) : CarEvent

@Serializable
public data class CarDrivenEvent(
    val distance: Double,
) : CarEvent

@Serializable
public data class CarEliminatedEvent(
    val reason: EliminationReason,
) : CarEvent

@Serializable
public enum class EliminationReason {
    Unfixable,
    Lost,
    Stolen,
}
