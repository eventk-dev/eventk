package dev.eventk.example.domain.process

interface CarProductionNotifier {
    fun notify(vin: String, make: String, model: String)
}
