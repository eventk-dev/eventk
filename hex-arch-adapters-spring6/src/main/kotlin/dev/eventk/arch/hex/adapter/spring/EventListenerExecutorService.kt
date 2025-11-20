package dev.eventk.arch.hex.adapter.spring

import dev.eventk.arch.hex.adapter.common.EventBatchTemplate
import dev.eventk.arch.hex.adapter.common.EventListenerExecutorConfig
import dev.eventk.arch.hex.adapter.common.multiStreamTypeEventFlow
import dev.eventk.arch.hex.adapter.common.singleStreamTypeEventFlow
import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.MultiStreamTypeEventListener
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.store.api.blocking.EventStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import kotlin.IllegalArgumentException

@Component
public class EventListenerExecutorService(
    private val eventStores: List<EventStore>,
    private val bookmark: Bookmark,
    private val singleStreamTypeEventListeners: List<SingleStreamTypeEventListener<*, *>>,
    private val multiStreamTypeEventListeners: List<MultiStreamTypeEventListener<*, *>>,
    private val config: EventListenerExecutorConfig = EventListenerExecutorConfig(),
    private val template: EventBatchTemplate = EventBatchTemplate.NoOp(),
) : InitializingBean, DisposableBean {
    private val logger: Logger = LoggerFactory.getLogger(EventListenerExecutorService::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    private val dispatcher = newFixedThreadPoolContext(config.threadPoolSize, config.threadPoolName)
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisor)

    private var stopped = false

    private val jobs = mutableMapOf<String, Job>()

    init {
        val allEventListeners = singleStreamTypeEventListeners + multiStreamTypeEventListeners
        if (allEventListeners.distinctBy { it.id }.size != allEventListeners.size) {
            val listenersWithDuplicatedId = allEventListeners
                .groupBy { it.id }
                .filter { it.value.count() > 1 }
                .mapValues { duplicatedId -> duplicatedId.value.map { it::class.simpleName } }
            throw IllegalStateException("The following event listeners have an id that is not unique: $listenersWithDuplicatedId")
        }
    }

    private fun init() {
        logger.info(
            "Starting listener processes for ${singleStreamTypeEventListeners.size} single event listeners " +
                "and ${multiStreamTypeEventListeners.size} multi event listeners...",
        )

        singleStreamTypeEventListeners.forEach { genericListener ->
            @Suppress("UNCHECKED_CAST")
            val eventListener = genericListener as SingleStreamTypeEventListener<Any, Any>
            jobs[eventListener.id] = startJob(eventListener)
        }
        multiStreamTypeEventListeners.forEach { genericListener ->
            @Suppress("UNCHECKED_CAST")
            val eventListener = genericListener as MultiStreamTypeEventListener<Any, Any>
            jobs[eventListener.id] = startJob(eventListener)
        }
    }

    private fun startJob(eventListener: SingleStreamTypeEventListener<Any, Any>): Job {
        val eventStore = eventStores
            .singleOrNull { eventListener.streamType in it.registeredTypes }
            ?: throw IllegalStateException("$eventListener has a stream type which needs to be registered in one (and only one) event store.")
        return scope.launch {
            var retry = 0
            while (!stopped) {
                logger.info("Starting collection of events for $eventListener")
                try {
                    eventStore
                        .singleStreamTypeEventFlow(
                            streamType = eventListener.streamType,
                            sincePosition = bookmark.get(eventListener.id),
                            batchSize = config.batchSize,
                        )
                        .collect { envelope ->
                            template.execute(eventStore, eventListener) {
                                eventListener.listen(envelope)
                                bookmark.set(eventListener.id, envelope.position)
                            }
                            if (retry > 0) retry = 0
                            logger.debug("Processed event position {} of type {} in {}", envelope.position, envelope.event::class.qualifiedName, eventListener)
                        }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val backoff = config.errorBackoff.backoff(++retry)
                    logger.error("Error while collecting events in $eventListener, will try to restart in $backoff", e)
                    if (!stopped) delay(backoff)
                }
            }
        }
    }

    private fun startJob(eventListener: MultiStreamTypeEventListener<Any, Any>): Job {
        val eventStore = eventStores
            .singleOrNull { es -> eventListener.streamTypes.all { st -> st in es.registeredTypes } }
            ?: throw IllegalStateException("$eventListener has a stream type which needs to be registered in one (and only one) event store.")
        return scope.launch {
            var retry = 0
            while (!stopped) {
                logger.info("Starting collection of events for $eventListener")
                try {
                    eventStore
                        .multiStreamTypeEventFlow(
                            sincePosition = bookmark.get(eventListener.id),
                            batchSize = config.batchSize,
                        )
                        .collect { envelope ->
                            template.execute(eventStore, eventListener) {
                                eventListener.listen(envelope)
                                bookmark.set(eventListener.id, envelope.position)
                            }
                            if (retry > 0) retry = 0
                            logger.debug("Processed event position {} of type {} in {}", envelope.position, envelope.event::class.qualifiedName, eventListener)
                        }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val backoff = config.errorBackoff.backoff(++retry)
                    logger.error("Error while collecting events in $eventListener, will try to restart in $backoff", e)
                    if (!stopped) delay(backoff)
                }
            }
        }
    }

    private fun shutdown() {
        logger.info("Shutting down...")
        stopped = true
        supervisor.cancel("Shutting down")
        dispatcher.close()
    }

    override fun afterPropertiesSet() {
        init()
    }

    override fun destroy() {
        shutdown()
    }

    public fun stopEventListener(id: String): Job {
        if (stopped) return Job().also { it.complete() }
        val job = jobs[id] ?: throw IllegalArgumentException("No current jobs for event listener '$id'")
        job.cancel()
        return job
    }

    public fun startEventListener(id: String) {
        val eventListener = (singleStreamTypeEventListeners + multiStreamTypeEventListeners).single { it.id == id }
        @Suppress("UNCHECKED_CAST")
        jobs[eventListener.id] = when (eventListener) {
            is MultiStreamTypeEventListener<*, *> -> startJob(eventListener as MultiStreamTypeEventListener<Any, Any>)
            is SingleStreamTypeEventListener<*, *> -> startJob(eventListener as SingleStreamTypeEventListener<Any, Any>)
        }
    }

    public suspend fun restartEventListener(id: String) {
        stopEventListener(id).join()
        startEventListener(id)
    }
}
