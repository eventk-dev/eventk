package dev.eventk.arch.hex.adapter.common

import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.EventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.blocking.EventStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

public fun CoroutineScope.launchListener(
    eventListener: EventListener,
    eventStore: EventStore,
    bookmark: Bookmark,
    observer: Observer,
    template: EventBatchTemplate,
    errorBackoff: BackoffStrategy,
    readBatchSize: Int,
    shouldStop: () -> Boolean,
): Job = launch {
    var retry = 0
    while (!shouldStop()) {
        observer.started(eventListener)
        try {
            eventStore
                .listenerEventFlow(
                    eventListener = eventListener,
                    sincePosition = bookmark.get(eventListener.id),
                    batchSize = readBatchSize,
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
            val backoff = errorBackoff.backoff(++retry)
            when (e) {
                is EnvelopeCollectionException -> observer.envelopeFailed(eventListener, e.envelope, e.cause!!, backoff)
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
