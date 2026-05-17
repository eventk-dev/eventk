package dev.eventk.arch.hex.adapter.common

import dev.eventk.arch.hex.port.EventListener
import dev.eventk.arch.hex.port.MultiStreamTypeEventListener
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType
import dev.eventk.store.api.blocking.EventStore
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.time.Duration.Companion.milliseconds

public fun EventStore.listenerEventFlow(
    eventListener: EventListener,
    sincePosition: Long,
    batchSize: Int,
): Flow<EventEnvelope<Any, Any>> = channelFlow {
    @Suppress("UNCHECKED_CAST")
    val loader: (Long) -> List<EventEnvelope<Any, Any>> = when (eventListener) {
        is SingleStreamTypeEventListener<*, *> -> { pos -> loadEventBatch(pos, batchSize, eventListener.streamType as StreamType<Any, Any>) }
        is MultiStreamTypeEventListener<*, *> -> { pos -> loadEventBatch(pos, batchSize) }
    }
    produce(sincePosition, loader, batchSize)
}

public fun <E, I> EventStore.singleStreamTypeEventFlow(
    streamType: StreamType<E, I>,
    sincePosition: Long,
    batchSize: Int,
): Flow<EventEnvelope<Any, Any>> = channelFlow {
    val loader: (Long) -> List<EventEnvelope<Any, Any>> = { pos -> loadEventBatch(pos, batchSize, streamType as StreamType<Any, Any>) }
    produce(sincePosition, loader, batchSize)
}

public fun EventStore.multiStreamTypeEventFlow(
    sincePosition: Long,
    batchSize: Int,
): Flow<EventEnvelope<Any, Any>> = channelFlow {
    val loader: (Long) -> List<EventEnvelope<Any, Any>> = { pos -> loadEventBatch(pos, batchSize) }
    produce(sincePosition, loader, batchSize)
}

private suspend fun ProducerScope<EventEnvelope<Any, Any>>.produce(
    sincePosition: Long,
    loader: (Long) -> List<EventEnvelope<Any, Any>>,
    batchSize: Int,
) {
    var lastPosition = sincePosition
    while (true) {
        val eventBatch = loader(lastPosition)
        eventBatch.forEach {
            send(it)
            lastPosition = it.position
        }
        if (eventBatch.isEmpty()) delay(500.milliseconds)
        if (eventBatch.size < batchSize) delay(250.milliseconds)
    }
}

@Suppress("UNCHECKED_CAST")
public fun EventListener.listen(envelope: EventEnvelope<Any, Any>): Unit = when (this) {
    is SingleStreamTypeEventListener<*, *> -> (this as SingleStreamTypeEventListener<Any, Any>).listen(envelope)
    is MultiStreamTypeEventListener<*, *> -> (this as MultiStreamTypeEventListener<Any, Any>).listen(envelope)
}
