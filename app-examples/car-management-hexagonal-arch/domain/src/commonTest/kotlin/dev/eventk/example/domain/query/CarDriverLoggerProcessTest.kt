package dev.eventk.example.domain.query

import dev.eventk.example.domain.Logger
import dev.eventk.example.domain.process.CarDriverLogger
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarStreamType
import dev.eventk.store.test.w.driver.DriverRegisteredEvent
import dev.eventk.store.test.w.driver.DriverStreamType
import kotlin.test.Test
import kotlin.test.assertEquals

class CarDriverLoggerProcessTest {
    @Test
    fun `given two different streams - when listening events - then both events are received`() {
        val logs = mutableListOf<String>()
        val logger = CarDriverLogger(
            object : Logger {
                override fun log(message: String) {
                    logs += message
                }
            },
        )
        logger.listen(
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
        logger.listen(
            EventEnvelope(
                streamType = DriverStreamType,
                streamId = driver1StreamId,
                version = 1,
                position = 2,
                metadata = emptyMap(),
                event = DriverRegisteredEvent(
                    licence = "1",
                    name = "Mr. Driver",
                ),
            ),
        )

        assertEquals(2, logs.size)
    }
}
