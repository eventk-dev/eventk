package dev.eventk.arch.hex.port

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType

public interface MultiStreamTypeEventListener<E, I> : EventListener {
    public val streamTypes: List<StreamType<out E, out I>>
    public fun listen(envelope: EventEnvelope<E, I>)
}
