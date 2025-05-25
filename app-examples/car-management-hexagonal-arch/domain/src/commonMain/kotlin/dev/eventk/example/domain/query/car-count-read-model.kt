package dev.eventk.example.domain.query

import com.benasher44.uuid.Uuid

data class MakeModelCar(
    val id: Uuid,
    val make: String,
    val model: String,
)

interface MakeModelCarRepository {
    fun find(id: Uuid): MakeModelCar?
    fun add(car: MakeModelCar)
    fun removeById(id: Uuid)
}

data class MakeModelCarCount(
    val make: String,
    val model: String,
    val count: Int,
)

interface MakeModelCarCountRepository {
    fun listAll(): List<MakeModelCarCount>
    fun find(make: String, model: String): MakeModelCarCount?
    fun save(carCount: MakeModelCarCount)
}
