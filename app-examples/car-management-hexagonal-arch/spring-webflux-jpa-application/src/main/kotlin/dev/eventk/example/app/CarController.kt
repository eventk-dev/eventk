package dev.eventk.example.app

import com.benasher44.uuid.uuidFrom
import dev.eventk.example.domain.usecases.blocking.CarProduction
import dev.eventk.example.domain.command.CarCommand
import dev.eventk.example.domain.query.MakeModelCarCountRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CarController(
    val carProduction: CarProduction,
    val carCountRepository: MakeModelCarCountRepository,
) {
    @GetMapping("/car-count/summary")
    fun getCarCountSummary(): List<CarCount> {
        return carCountRepository.listAll().map {
            CarCount(
                make = it.make,
                model = it.model,
                count = it.count,
            )
        }
    }

    @PostMapping("/cars")
    fun postCar(
        @RequestBody body: ProduceCarRequest,
    ) {
        carProduction.produceCar(
            CarCommand.Produce(
                uuidFrom(body.id),
                body.vin,
                body.producer,
                body.make,
                body.model,
            ),
        )
    }

    data class ProduceCarRequest(
        val id: String,
        val vin: String,
        val producer: Long,
        val make: String,
        val model: String,
    )

    data class CarCount(
        val make: String,
        val model: String,
        val count: Int,
    )
}
