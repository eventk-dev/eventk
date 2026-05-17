package dev.eventk.arch.hex.adapter.ktor

import dev.eventk.arch.hex.adapter.common.EventBatchTemplate
import dev.eventk.arch.hex.adapter.common.EventListenerExecutorConfig
import dev.eventk.arch.hex.adapter.common.Logger
import dev.eventk.arch.hex.adapter.common.startJob
import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.EventListener
import dev.eventk.arch.hex.port.MultiStreamTypeEventListener
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.store.api.blocking.EventStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.newFixedThreadPoolContext

public class EventListenerExecutorService(
    private val eventStores: List<EventStore>,
    private val bookmark: Bookmark,
    private val singleStreamTypeEventListeners: List<SingleStreamTypeEventListener<*, *>>,
    private val multiStreamTypeEventListeners: List<MultiStreamTypeEventListener<*, *>>,
    private val config: EventListenerExecutorConfig = EventListenerExecutorConfig(),
    private val template: EventBatchTemplate = EventBatchTemplate.NoOp(),
    private val debugLogger: (messageGenerator: () -> String) -> Unit,
    private val infoLogger: (messageGenerator: () -> String) -> Unit,
    private val errorLogger: (t: Throwable, messageGenerator: () -> String) -> Unit,
) {
    private val logger: Logger = object : Logger {
        override fun info(message: () -> String) = infoLogger(message)
        override fun debug(message: () -> String) = debugLogger(message)
        override fun error(t: Throwable, message: () -> String) = errorLogger(t, message)
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
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

    public fun init() {
        logger.info {
            "Starting listener processes for ${singleStreamTypeEventListeners.size} single event listeners " +
                "and ${multiStreamTypeEventListeners.size} multi event listeners..."
        }

        singleStreamTypeEventListeners.forEach { listener ->
            @Suppress("UNCHECKED_CAST")
            jobs[listener.id] = startJob(listener as SingleStreamTypeEventListener<Any, Any>)
        }
        multiStreamTypeEventListeners.forEach { listener ->
            @Suppress("UNCHECKED_CAST")
            jobs[listener.id] = startJob(listener as MultiStreamTypeEventListener<Any, Any>)
        }
    }

    private fun startJob(eventListener: SingleStreamTypeEventListener<Any, Any>): Job {
        val eventStore = eventStores
            .singleOrNull { eventListener.streamType in it.registeredTypes }
            ?: throw IllegalStateException("$eventListener has a stream type which needs to be registered in one (and only one) event store.")
        return startJob(eventListener, eventStore)
    }

    private fun startJob(eventListener: MultiStreamTypeEventListener<Any, Any>): Job {
        val eventStore = eventStores
            .singleOrNull { es -> eventListener.streamTypes.all { st -> st in es.registeredTypes } }
            ?: throw IllegalStateException("$eventListener has a stream type which needs to be registered in one (and only one) event store.")
        return startJob(eventListener, eventStore)
    }

    private fun startJob(eventListener: EventListener, eventStore: EventStore): Job =
        startJob(scope, eventListener, eventStore, bookmark, logger, template, config.errorBackoff, config.batchSize) { stopped }

    public fun shutdown() {
        logger.info { "Shutting down..." }
        stopped = true
        supervisor.cancel("Shutting down")
        dispatcher.close()
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
