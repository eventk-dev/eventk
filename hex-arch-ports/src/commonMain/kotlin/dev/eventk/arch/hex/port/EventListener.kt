package dev.eventk.arch.hex.port

public sealed interface EventListener {
    public val id: String
}

public sealed interface SingleEventListener : EventListener

public sealed interface BatchEventListener : EventListener
