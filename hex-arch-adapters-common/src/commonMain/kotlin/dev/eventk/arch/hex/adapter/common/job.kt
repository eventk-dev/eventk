package dev.eventk.arch.hex.adapter.common

import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.EventListener
import dev.eventk.store.api.blocking.EventStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

public fun startJob(
    scope: CoroutineScope,
    eventListener: EventListener,
    eventStore: EventStore,
    bookmark: Bookmark,
    logger: Logger,
    template: EventBatchTemplate,
    errorBackoff: BackoffStrategy,
    readBatchSize: Int,
    isStopped: () -> Boolean,
): Job = scope.launch {
    var retry = 0
    while (!isStopped()) {
        logger.info { "Starting collection of events for $eventListener" }
        try {
            eventStore
                .listenerEventFlow(
                    eventListener = eventListener,
                    sincePosition = bookmark.get(eventListener.id),
                    batchSize = readBatchSize,
                )
                .collect { envelope ->
                    template.execute(eventStore, eventListener) {
                        eventListener.listen(envelope)
                        bookmark.set(eventListener.id, envelope.position)
                    }
                    if (retry > 0) retry = 0
                    logger.debug { "Processed event position ${envelope.position} of type ${envelope.event::class.simpleName} in $eventListener" }
                }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val backoff = errorBackoff.backoff(++retry)
            logger.error(e) { "Error while collecting events in $eventListener, will try to restart in $backoff" }
            if (!isStopped()) delay(backoff)
        }
    }
}
