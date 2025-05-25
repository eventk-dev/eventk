package dev.eventk.arch.hex.port

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType

public interface SingleStreamTypeEventListener<E, I> : EventListener {
    public val streamType: StreamType<E, I>
    public fun listen(envelope: EventEnvelope<E, I>)
}
