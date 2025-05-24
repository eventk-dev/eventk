package dev.eventk.example.domain.usecases.blocking

import com.benasher44.uuid.Uuid
import dev.eventk.example.domain.UnitOfWork
import dev.eventk.example.domain.UseCase
import dev.eventk.example.domain.command.Car
import dev.eventk.example.domain.command.CarCommand
import dev.eventk.example.domain.command.CarWriteHelperBlockingRepository
import dev.eventk.example.domain.command.handle
import dev.eventk.store.api.blocking.EventStore
import dev.eventk.store.test.w.car.CarEvent
import dev.eventk.store.test.w.car.CarStreamType

@UseCase
class CarProduction(
    private val carWriteHelperRepository: CarWriteHelperBlockingRepository,
    private val eventStore: EventStore,
    private val unitOfWork: UnitOfWork,
) {
    fun produceCar(command: CarCommand.Produce): Uuid = unitOfWork.mark {
        val writeSideCar = carWriteHelperRepository.getOrCreate(command.id, command.vin)
        if (writeSideCar.id != command.id) {
            return@mark writeSideCar.id
        }

        val carStreamHandler = eventStore.withStreamType(CarStreamType)

        val loadResult = carStreamHandler.loadStream(writeSideCar.id)

        val car = loadResult
            .map { it.event }
            .fold(Car(writeSideCar.id)) { s: Car, e: CarEvent -> s + e }

        val events = car.handle(command)

        carStreamHandler.appendStream(writeSideCar.id, car.version, events)

        writeSideCar.id
    }
}
