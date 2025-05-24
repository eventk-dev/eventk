package dev.eskt.example.app

import com.benasher44.uuid.uuid4
import dev.eskt.example.domain.usecases.suspending.CarProduction
import dev.eskt.example.domain.command.CarCommand
import dev.eskt.example.domain.command.CarWriteHelperSuspendingRepository
import dev.eskt.store.api.suspending.EventStore
import dev.eskt.store.test.w.car.CarProducedEvent
import dev.eskt.store.test.w.car.CarStreamType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest
internal class CarProductionIntegrationTest(
    @Autowired
    val flyway: Flyway,
    @Autowired
    val carWriteHelperRepository: CarWriteHelperSuspendingRepository,
    @Autowired
    val eventStore: EventStore,
    @Autowired
    val carProduction: CarProduction,
) {
    @BeforeEach
    fun setup() {
        flyway.clean()
        flyway.migrate()
    }

    @Test
    fun `given no cars, when car is produced, then event is generated`() = runTest {
        // given

        // when
        val id = uuid4()
        carProduction.produceCar(
            CarCommand.Produce(
                id = id,
                vin = "1FDKF37GXVEB30871",
                producer = 1L,
                make = "Kia",
                model = "Rio",
            ),
        )

        // then
        val events = eventStore
            .withStreamType(CarStreamType)
            .loadStream(id)
            .map { it.event }
        val writeSideCar = carWriteHelperRepository.getByUuid(id)

        assertEquals(1, events.size)
        assertEquals(id, writeSideCar?.id)
        assertIs<CarProducedEvent>(events[0]).also { event ->
            assertEquals(writeSideCar?.vin, event.vin)
        }
    }

    @Test
    fun `given car already exists, when car is produced, then previous id is returned`() = runTest {
        // given
        val existingId = uuid4()
        val existingVin = "1FDKF37GXVEB30871"
        carWriteHelperRepository.getOrCreate(existingId, existingVin)
        eventStore
            .withStreamType(CarStreamType)
            .appendStream(existingId, 0, listOf(CarProducedEvent(existingVin, 1, "Kia", "Rio")))

        // when
        val newId = uuid4()
        carProduction.produceCar(
            CarCommand.Produce(
                id = newId,
                vin = existingVin,
                producer = 1L,
                make = "Kia",
                model = "Rio",
            ),
        )

        // then
        val newEvents = eventStore
            .withStreamType(CarStreamType)
            .loadStream(newId)
            .map { it.event }
        val writeSideCar = carWriteHelperRepository.getByUuid(existingId)
        val newWriteSideCar = carWriteHelperRepository.getByUuid(newId)

        assertEquals(0, newEvents.size)
        assertNull(newWriteSideCar)
        assertEquals(existingId, writeSideCar?.id)
        assertEquals(existingVin, writeSideCar?.vin)
    }
}
