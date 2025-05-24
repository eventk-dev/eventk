package dev.eventk.example.app

import dev.eventk.example.domain.Logger
import org.springframework.stereotype.Component

@Component
data object StdOutLogger : Logger {
    override fun log(message: String) {
        println(message)
    }
}
