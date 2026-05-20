package dev.eventk.arch.hex.adapter.spring

import dev.eventk.arch.hex.adapter.common.EventBatchTemplate
import dev.eventk.arch.hex.adapter.common.EventListenerExecutorConfig
import dev.eventk.arch.hex.adapter.common.Observer
import dev.eventk.arch.hex.adapter.common.launchListener
import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.EventListener
import dev.eventk.arch.hex.port.MultiStreamTypeEventListener
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.blocking.EventStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.newFixedThreadPoolContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import kotlin.IllegalArgumentException
import kotlin.time.Duration

@Component
public class EventListenerExecutorService(
    private val eventStores: List<EventStore>,
    private val bookmark: Bookmark,
    private val singleStreamTypeEventListeners: List<SingleStreamTypeEventListener<*, *>>,
    private val multiStreamTypeEventListeners: List<MultiStreamTypeEventListener<*, *>>,
    private val config: EventListenerExecutorConfig = EventListenerExecutorConfig(),
    private val template: EventBatchTemplate = EventBatchTemplate.NoOp(),
) : InitializingBean, DisposableBean {
    private val slf4jLogger = LoggerFactory.getLogger(EventListenerExecutorService::class.java)
    private val observer = object : Observer {
        override fun started(eventListener: EventListener) {
            if (slf4jLogger.isInfoEnabled) slf4jLogger.info("Starting collection of events for $eventListener")
        }

        override fun finished(eventListener: EventListener) {
            if (slf4jLogger.isInfoEnabled) slf4jLogger.info("Finished collection of events for $eventListener")
        }

        override fun envelopeCompleted(eventListener: EventListener, envelope: EventEnvelope<Any, Any>) {
            if (slf4jLogger.isDebugEnabled) slf4jLogger.debug("Collected $envelope in $eventListener")
        }
        override fun envelopeFailed(eventListener: EventListener, envelope: EventEnvelope<Any, Any>, t: Throwable, backoff: Duration) {
            slf4jLogger.error("Error while collecting $envelope in $eventListener, will try to restart in $backoff", t)
        }
        override fun failed(eventListener: EventListener, t: Throwable, backoff: Duration) {
            slf4jLogger.error("Error while collecting flow in $eventListener, will try to restart in $backoff", t)
        }
    }

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
        slf4jLogger.info(
            "Starting listener processes for ${singleStreamTypeEventListeners.size} single event listeners " +
                "and ${multiStreamTypeEventListeners.size} multi event listeners...",
        )

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
        scope.launchListener(eventListener, eventStore, bookmark, observer, template, config.errorBackoff, config.batchSize) { stopped }

    private fun shutdown() {
        slf4jLogger.info("Shutting down...")
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
