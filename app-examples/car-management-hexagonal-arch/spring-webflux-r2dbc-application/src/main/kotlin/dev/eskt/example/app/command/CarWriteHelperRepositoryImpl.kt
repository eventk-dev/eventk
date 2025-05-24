package dev.eskt.example.app.command

import com.benasher44.uuid.Uuid
import dev.eskt.example.domain.command.CarWriteHelper
import dev.eskt.example.domain.command.CarWriteHelperSuspendingRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*

@Repository
interface CarWriteHelperRepositoryImpl : ReactiveCrudRepository<CarWriteHelperEntity, Uuid>, CarWriteHelperSuspendingRepository {
    fun findByVin(vin: String): Mono<CarWriteHelperEntity>

    override suspend fun getByUuid(id: Uuid): CarWriteHelper? {
        return findById(id)
            .awaitSingleOrNull()
            ?.let { CarWriteHelper(it.id, it.vin) }
    }

    override suspend fun getOrCreate(id: Uuid, vin: String): CarWriteHelper {
        val existing = findByVin(vin).awaitSingleOrNull()
        if (existing != null) {
            return CarWriteHelper(existing.id, existing.vin)
        }

        val savedEntity = save(CarWriteHelperEntity(id, vin)).awaitSingle()
        return CarWriteHelper(savedEntity.id, savedEntity.vin)
    }
}
