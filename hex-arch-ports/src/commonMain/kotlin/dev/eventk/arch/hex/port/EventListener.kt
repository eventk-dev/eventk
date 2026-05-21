package dev.eventk.arch.hex.port

public sealed interface EventListener {
    public val id: String
}

public sealed interface SingleEventListener : EventListener {
    public val listenerConfig: SingleListenerConfig? get() = null
}

public sealed interface BatchEventListener : EventListener {
    public val listenerConfig: BatchListenerConfig? get() = null
}
