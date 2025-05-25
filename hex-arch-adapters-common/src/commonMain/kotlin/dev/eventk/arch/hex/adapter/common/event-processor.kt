package dev.eventk.arch.hex.adapter.common

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType
import dev.eventk.store.api.blocking.EventStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

public fun <E, I> EventStore.singleStreamTypeEventFlow(
    streamType: StreamType<E, I>,
    sincePosition: Long,
    batchSize: Int,
): Flow<EventEnvelope<E, I>> = channelFlow {
    var lastPosition = sincePosition
    while (true) {
        val eventBatch = loadEventBatch(lastPosition, batchSize, streamType = streamType)
        eventBatch.forEach {
            this.send(it)
            lastPosition = it.position
        }
        if (eventBatch.isEmpty()) delay(500)
        if (eventBatch.size < batchSize) delay(250)
    }
}

public fun EventStore.multiStreamTypeEventFlow(
    sincePosition: Long,
    batchSize: Int,
): Flow<EventEnvelope<Any, Any>> = channelFlow {
    var lastPosition = sincePosition
    while (true) {
        val eventBatch = loadEventBatch<Any, Any>(lastPosition, batchSize)
        eventBatch.forEach {
            this.send(it)
            lastPosition = it.position
        }
        if (eventBatch.isEmpty()) delay(500)
        if (eventBatch.size < batchSize) delay(250)
    }
}
