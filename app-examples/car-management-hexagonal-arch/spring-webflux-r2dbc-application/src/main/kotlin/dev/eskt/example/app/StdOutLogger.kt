package dev.eskt.example.app

import dev.eskt.example.domain.Logger
import org.springframework.stereotype.Component

@Component
data object StdOutLogger : Logger {
    override fun log(message: String) {
        println(message)
    }
}
