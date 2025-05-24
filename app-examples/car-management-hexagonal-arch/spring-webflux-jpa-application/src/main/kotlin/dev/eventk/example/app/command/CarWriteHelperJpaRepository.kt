package dev.eventk.example.app.command

import com.benasher44.uuid.Uuid
import dev.eventk.example.domain.command.CarWriteHelper
import dev.eventk.example.domain.command.CarWriteHelperBlockingRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
interface CarWriteHelperJpaRepository : JpaRepository<CarWriteHelperEntity, Uuid>, CarWriteHelperBlockingRepository {
    fun findByVin(vin: String): CarWriteHelperEntity?

    override fun getByUuid(id: Uuid): CarWriteHelper? {
        return findByIdOrNull(id)
            ?.let { CarWriteHelper(it.id, it.vin) }
    }

    override fun getOrCreate(id: Uuid, vin: String): CarWriteHelper {
        val entity = findByVin(vin)
            ?: save(CarWriteHelperEntity(id, vin))
        return CarWriteHelper(entity.id, entity.vin)
    }
}
