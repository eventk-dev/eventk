package dev.eventk.arch.hex.adapter.common

import dev.eventk.arch.hex.port.EventListener
import dev.eventk.store.api.EventEnvelope
import kotlin.time.Duration

public interface Observer {
    public fun started(eventListener: EventListener)
    public fun finished(eventListener: EventListener)
    public fun envelopeCompleted(eventListener: EventListener, envelope: EventEnvelope<Any, Any>)
    public fun envelopeFailed(eventListener: EventListener, envelope: EventEnvelope<Any, Any>, t: Throwable, backoff: Duration)
    public fun failed(eventListener: EventListener, t: Throwable, backoff: Duration)
}
