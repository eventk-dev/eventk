package dev.eventk.arch.hex.adapter.common

import dev.eventk.arch.hex.port.BatchEventListener
import dev.eventk.arch.hex.port.EventListener
import dev.eventk.arch.hex.port.SingleEventListener
import dev.eventk.store.api.EventEnvelope
import kotlin.time.Duration

public interface Observer {
    public fun started(eventListener: EventListener)
    public fun finished(eventListener: EventListener)
    public fun envelopeCompleted(eventListener: SingleEventListener, envelope: EventEnvelope<Any, Any>)
    public fun envelopeFailed(eventListener: SingleEventListener, envelope: EventEnvelope<Any, Any>, t: Throwable, backoff: Duration)
    public fun batchCompleted(eventListener: BatchEventListener, envelopes: List<EventEnvelope<Any, Any>>): Unit = Unit
    public fun batchFailed(eventListener: BatchEventListener, envelopes: List<EventEnvelope<Any, Any>>, t: Throwable, backoff: Duration): Unit = Unit
    public fun failed(eventListener: EventListener, t: Throwable, backoff: Duration)
}
