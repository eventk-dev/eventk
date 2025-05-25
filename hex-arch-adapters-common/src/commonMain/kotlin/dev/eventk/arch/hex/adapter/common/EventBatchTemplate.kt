package dev.eventk.arch.hex.adapter.common

import dev.eventk.arch.hex.port.EventListener
import dev.eventk.store.api.blocking.EventStore

public interface EventBatchTemplate {
    public fun execute(eventStore: EventStore, eventListener: EventListener, block: () -> Unit)

    public class NoOp : EventBatchTemplate {
        override fun execute(eventStore: EventStore, eventListener: EventListener, block: () -> Unit) {
            block.invoke()
        }
    }
}
