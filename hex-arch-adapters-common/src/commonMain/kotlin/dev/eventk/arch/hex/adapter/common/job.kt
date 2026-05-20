package dev.eventk.arch.hex.adapter.common

import dev.eventk.arch.hex.port.BatchEventListener
import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.SingleEventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.blocking.EventStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

public fun CoroutineScope.launchListener(
    eventListener: SingleEventListener,
    eventStore: EventStore,
    bookmark: Bookmark,
    observer: Observer,
    template: EventBatchTemplate,
    backoff: BackoffStrategy,
    readBatchSize: Int,
    shouldStop: () -> Boolean,
): Job = launch {
    val effectiveBatchSize = eventListener.listenerConfig?.readBatchSize ?: readBatchSize
    var retry = 0
    while (!shouldStop()) {
        observer.started(eventListener)
        try {
            eventStore
                .listenerEventFlow(
                    eventListener = eventListener,
                    sincePosition = bookmark.get(eventListener.id),
                    batchSize = effectiveBatchSize,
                )
                .collect { envelope ->
                    try {
                        template.execute(eventStore, eventListener) {
                            eventListener.listen(envelope)
                            bookmark.set(eventListener.id, envelope.position)
                        }
                        observer.envelopeCompleted(eventListener, envelope)
                        if (retry > 0) retry = 0
                    } catch (e: RuntimeException) {
                        throw EnvelopeCollectionException(envelope, e)
                    }
                }
        } catch (e: RuntimeException) {
            if (e is CancellationException) throw e
            val backoff = backoff.backoff(++retry)
            when (e) {
                is EnvelopeCollectionException -> observer.envelopeFailed(eventListener, e.envelope, e.cause!!, backoff)
                else -> observer.failed(eventListener, e, backoff)
            }
            if (!shouldStop()) delay(backoff)
        }
    }
    observer.finished(eventListener)
}

public fun CoroutineScope.launchBatchListener(
    eventListener: BatchEventListener,
    eventStore: EventStore,
    bookmark: Bookmark,
    observer: Observer,
    template: EventBatchTemplate,
    backoff: BackoffStrategy,
    readBatchSize: Int,
    writeBatchSize: Int,
    batchTimeout: Duration,
    shouldStop: () -> Boolean,
): Job = launch {
    val effectiveBatchSize = eventListener.listenerConfig?.readBatchSize ?: readBatchSize
    val effectiveWriteBatchSize = eventListener.listenerConfig?.writeBatchSize ?: writeBatchSize
    val effectiveBatchTimeout = eventListener.listenerConfig?.batchTimeout ?: batchTimeout
    var retry = 0
    while (!shouldStop()) {
        observer.started(eventListener)
        try {
            eventStore
                .listenerEventFlow(
                    eventListener = eventListener,
                    sincePosition = bookmark.get(eventListener.id),
                    batchSize = effectiveBatchSize,
                )
                .chunkedWithTimeout(effectiveWriteBatchSize, effectiveBatchTimeout)
                .collect { envelopes ->
                    try {
                        template.execute(eventStore, eventListener) {
                            eventListener.listenBatch(envelopes)
                            bookmark.set(eventListener.id, envelopes.last().position)
                        }
                        observer.batchCompleted(eventListener, envelopes)
                        if (retry > 0) retry = 0
                    } catch (e: RuntimeException) {
                        throw BatchCollectionException(envelopes, e)
                    }
                }
        } catch (e: RuntimeException) {
            if (e is CancellationException) throw e
            val backoff = backoff.backoff(++retry)
            when (e) {
                is BatchCollectionException -> observer.batchFailed(eventListener, e.envelopes, e.cause!!, backoff)
                else -> observer.failed(eventListener, e, backoff)
            }
            if (!shouldStop()) delay(backoff)
        }
    }
    observer.finished(eventListener)
}

private class EnvelopeCollectionException(public val envelope: EventEnvelope<Any, Any>, cause: Throwable) :
    RuntimeException(
        "Error while collecting $envelope",
        cause,
    )

private class BatchCollectionException(public val envelopes: List<EventEnvelope<Any, Any>>, cause: Throwable) :
    RuntimeException(
        "Error while collecting batch of ${envelopes.size} envelopes ending at position ${envelopes.last().position}",
        cause,
    )