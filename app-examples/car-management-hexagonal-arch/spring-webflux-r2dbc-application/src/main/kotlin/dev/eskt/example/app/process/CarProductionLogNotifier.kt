package dev.eskt.example.app.process

import dev.eskt.example.domain.process.CarProductionNotifier
import org.springframework.stereotype.Component

@Component
class CarProductionLogNotifier : CarProductionNotifier {
    override fun notify(vin: String, make: String, model: String) {
        println("produced $make $model car with VIN $vin")
    }
}
