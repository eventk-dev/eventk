package dev.eventk.arch.hex.port

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType

public interface SingleStreamTypeBatchEventListener<E, I> : BatchEventListener {
    public val streamType: StreamType<E, I>
    public fun listen(envelopes: List<EventEnvelope<E, I>>)
}
