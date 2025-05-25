package dev.eventk.example.domain.query

import com.benasher44.uuid.Uuid
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.test.w.car.CarEliminatedEvent
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarStreamType
import dev.eventk.store.test.w.car.EliminationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class CarCountEventListenerTest {
    @Test
    fun `given no cars - when listening to first production event - then read model updates correctly`() {
        // given
        val countRepo = MakeModelCarCountMemoryRepository()
        val carRepo = MakeModelCarMemoryRepository()

        // when
        val listener = CarCountEventListener(countRepo, carRepo)
        listener.listen(
            EventEnvelope(
                streamType = CarStreamType,
                streamId = car1StreamId,
                version = 1,
                position = 1,
                metadata = emptyMap(),
                event = CarProducedEvent(
                    vin = "2A4RR5D1XAR410299",
                    make = "Kia",
                    model = "Rio",
                    producer = 1,
                ),
            ),
        )

        // then
        assertEquals(1, countRepo.listAll().size)
        assertEquals(1, countRepo.find("Kia", "Rio")?.count)
        assertNotNull(carRepo.find(car1StreamId)).also { car ->
            assertEquals("Kia", car.make)
            assertEquals("Rio", car.model)
        }
    }

    @Test
    fun `given 1 car counted - when listening to the same production event - then nothing happens`() {
        // given
        val countRepo = MakeModelCarCountMemoryRepository(
            MakeModelCarCount("Kia", "Rio", 1),
        )
        val carRepo = MakeModelCarMemoryRepository(
            MakeModelCar(car1StreamId, "Kia", "Rio"),
        )

        // when
        val listener = CarCountEventListener(countRepo, carRepo)
        listener.listen(
            EventEnvelope(
                streamType = CarStreamType,
                streamId = car1StreamId,
                version = 1,
                position = 1,
                metadata = emptyMap(),
                event = CarProducedEvent(
                    vin = "2A4RR5D1XAR410299",
                    make = "Kia",
                    model = "Rio",
                    producer = 1,
                ),
            ),
        )

        // then
        assertEquals(1, countRepo.listAll().size)
        assertEquals(1, countRepo.find("Kia", "Rio")?.count)
        assertNotNull(carRepo.find(car1StreamId)).also { car ->
            assertEquals("Kia", car.make)
            assertEquals("Rio", car.model)
        }
    }

    @Test
    fun `given 1 car counted - when listening to second production event - then read model updates correctly`() {
        // given
        val countRepo = MakeModelCarCountMemoryRepository(
            MakeModelCarCount("Kia", "Rio", 1),
        )
        val carRepo = MakeModelCarMemoryRepository(
            MakeModelCar(car1StreamId, "Kia", "Rio"),
        )

        // when
        val listener = CarCountEventListener(countRepo, carRepo)
        listener.listen(
            EventEnvelope(
                streamType = CarStreamType,
                streamId = car2StreamId,
                version = 1,
                position = 2,
                metadata = emptyMap(),
                event = CarProducedEvent(
                    vin = "1J8HG48K67C669063",
                    make = "Kia",
                    model = "Rio",
                    producer = 5,
                ),
            ),
        )

        // then
        assertEquals(1, countRepo.listAll().size)
        assertEquals(2, countRepo.find("Kia", "Rio")?.count)
        assertNotNull(carRepo.find(car2StreamId)).also { car ->
            assertEquals("Kia", car.make)
            assertEquals("Rio", car.model)
        }
    }

    @Test
    fun `given 1 car counted - when listening to the same car being eliminated - then read model updates correctly`() {
        // given
        val countRepo = MakeModelCarCountMemoryRepository(
            MakeModelCarCount("Kia", "Rio", 1),
        )
        val carRepo = MakeModelCarMemoryRepository(
            MakeModelCar(car1StreamId, "Kia", "Rio"),
        )

        // when
        val listener = CarCountEventListener(countRepo, carRepo)
        listener.listen(
            EventEnvelope(
                streamType = CarStreamType,
                streamId = car1StreamId,
                version = 2,
                position = 2,
                metadata = emptyMap(),
                event = CarEliminatedEvent(
                    reason = EliminationReason.Lost,
                ),
            ),
        )

        // then
        assertEquals(1, countRepo.listAll().size)
        assertEquals(0, countRepo.find("Kia", "Rio")?.count)
        assertNull(carRepo.find(car1StreamId))
    }

    @Test
    fun `given 2 cars counted - when listening to second production event - then read model updates correctly`() {
        // given
        val countRepo = MakeModelCarCountMemoryRepository(
            MakeModelCarCount("Kia", "Rio", 2),
        )
        val carRepo = MakeModelCarMemoryRepository(
            MakeModelCar(car1StreamId, "Kia", "Rio"),
            MakeModelCar(car2StreamId, "Kia", "Rio"),
        )

        // when
        val listener = CarCountEventListener(countRepo, carRepo)
        listener.listen(
            EventEnvelope(
                streamType = CarStreamType,
                streamId = car3StreamId,
                version = 1,
                position = 3,
                metadata = emptyMap(),
                event = CarProducedEvent(
                    vin = "1J4NT1GA3BD184938",
                    make = "Toyota",
                    model = "Corolla",
                    producer = 17,
                ),
            ),
        )

        // then
        assertEquals(2, countRepo.listAll().size)
        assertEquals(1, countRepo.find("Toyota", "Corolla")?.count)
        assertNotNull(carRepo.find(car3StreamId)).also { car ->
            assertEquals("Toyota", car.make)
            assertEquals("Corolla", car.model)
        }
    }

    class MakeModelCarCountMemoryRepository(vararg initialCars: MakeModelCarCount) : MakeModelCarCountRepository {
        private val cars = mutableListOf(*initialCars)

        override fun listAll(): List<MakeModelCarCount> = cars.toList()
        override fun find(make: String, model: String): MakeModelCarCount? = cars.singleOrNull { it.make == make && it.model == model }

        override fun save(carCount: MakeModelCarCount) {
            cars.removeAll { it.make == carCount.make && it.model == carCount.model }
            cars.add(carCount)
        }
    }

    class MakeModelCarMemoryRepository(vararg initialCars: MakeModelCar) : MakeModelCarRepository {
        private val cars = mutableListOf(*initialCars)

        override fun find(id: Uuid): MakeModelCar? = cars.singleOrNull { it.id == id }

        override fun add(car: MakeModelCar) {
            cars.add(car)
        }

        override fun removeById(id: Uuid) {
            cars.removeAll { it.id == id }
        }
    }
}
