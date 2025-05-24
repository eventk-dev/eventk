package dev.eskt.example.app.query

import com.benasher44.uuid.Uuid
import dev.eskt.example.domain.query.MakeModelCar
import dev.eskt.example.domain.query.MakeModelCarRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
interface MakeModelCarJdbcRepository : CrudRepository<MakeModelCarEntity, Uuid>, MakeModelCarRepository {
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
