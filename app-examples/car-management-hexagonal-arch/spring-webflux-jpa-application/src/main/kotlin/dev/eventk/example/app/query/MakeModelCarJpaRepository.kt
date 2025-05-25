package dev.eventk.example.app.query

import com.benasher44.uuid.Uuid
import dev.eventk.example.domain.query.MakeModelCar
import dev.eventk.example.domain.query.MakeModelCarRepository
import org.springframework.data.jpa.repository.JpaRepository
import kotlin.jvm.optionals.getOrNull

interface MakeModelCarJpaRepository : JpaRepository<MakeModelCarEntity, Uuid>, MakeModelCarRepository {
    override fun find(id: Uuid): MakeModelCar? {
        return findById(id).getOrNull()?.let { MakeModelCar(id = it.id, make = it.make, model = it.model) }
    }

    override fun add(car: MakeModelCar) {
        save(MakeModelCarEntity(id = car.id, make = car.make, model = car.model))
    }

    override fun removeById(id: Uuid) {
        deleteById(id)
    }
}
