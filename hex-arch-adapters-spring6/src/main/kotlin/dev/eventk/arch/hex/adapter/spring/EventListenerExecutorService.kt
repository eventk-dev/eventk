package dev.eventk.arch.hex.adapter.spring

import dev.eventk.arch.hex.adapter.common.EventBatchTemplate
import dev.eventk.arch.hex.adapter.common.EventListenerExecutorConfig
import dev.eventk.arch.hex.adapter.common.Observer
import dev.eventk.arch.hex.adapter.common.launchBatchListener
import dev.eventk.arch.hex.adapter.common.launchListener
import dev.eventk.arch.hex.port.BatchEventListener
import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.EventListener
import dev.eventk.arch.hex.port.MultiStreamTypeBatchEventListener
import dev.eventk.arch.hex.port.MultiStreamTypeEventListener
import dev.eventk.arch.hex.port.SingleEventListener
import dev.eventk.arch.hex.port.SingleStreamTypeBatchEventListener
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
    singleStreamTypeEventListeners: List<SingleStreamTypeEventListener<*, *>>,
    multiStreamTypeEventListeners: List<MultiStreamTypeEventListener<*, *>>,
    batchSingleStreamTypeEventListeners: List<SingleStreamTypeBatchEventListener<*, *>> = emptyList(),
    batchMultiStreamTypeEventListeners: List<MultiStreamTypeBatchEventListener<*, *>> = emptyList(),
    private val config: EventListenerExecutorConfig = EventListenerExecutorConfig(),
    private val template: EventBatchTemplate = EventBatchTemplate.NoOp(),
) : InitializingBean, DisposableBean {
    private val singleEventListeners: List<SingleEventListener> =
        singleStreamTypeEventListeners + multiStreamTypeEventListeners
    private val batchEventListeners: List<BatchEventListener> =
        batchSingleStreamTypeEventListeners + batchMultiStreamTypeEventListeners

    private val slf4jLogger = LoggerFactory.getLogger(EventListenerExecutorService::class.java)
    private val observer = object : Observer {
        override fun started(eventListener: EventListener) {
            if (slf4jLogger.isInfoEnabled) slf4jLogger.info("Starting collection of events for $eventListener")
        }
        override fun finished(eventListener: EventListener) {
            if (slf4jLogger.isInfoEnabled) slf4jLogger.info("Finished collection of events for $eventListener")
        }
        override fun envelopeCompleted(eventListener: SingleEventListener, envelope: EventEnvelope<Any, Any>) {
            if (slf4jLogger.isDebugEnabled) slf4jLogger.debug("Collected $envelope in $eventListener")
        }
        override fun envelopeFailed(eventListener: SingleEventListener, envelope: EventEnvelope<Any, Any>, t: Throwable, backoff: Duration) {
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
        val allListeners = singleEventListeners + batchEventListeners
        if (allListeners.distinctBy { it.id }.size != allListeners.size) {
            val listenersWithDuplicatedId = allListeners
                .groupBy { it.id }
                .filter { it.value.count() > 1 }
                .mapValues { duplicatedId -> duplicatedId.value.map { it::class.simpleName } }
            throw IllegalStateException("The following event listeners have an id that is not unique: $listenersWithDuplicatedId")
        }
    }

    private fun init() {
        slf4jLogger.info(
            "Starting listener processes for ${singleEventListeners.size} single event listeners " +
                "and ${batchEventListeners.size} batch event listeners...",
        )
        singleEventListeners.forEach { jobs[it.id] = startJob(it) }
        batchEventListeners.forEach { jobs[it.id] = startBatchJob(it) }
    }

    private fun startJob(eventListener: SingleEventListener): Job {
        @Suppress("UNCHECKED_CAST")
        val eventStore = when (eventListener) {
            is SingleStreamTypeEventListener<*, *> ->
                eventStores.singleOrNull { (eventListener as SingleStreamTypeEventListener<Any, Any>).streamType in it.registeredTypes }
            is MultiStreamTypeEventListener<*, *> ->
                eventStores.singleOrNull { es -> (eventListener as MultiStreamTypeEventListener<Any, Any>).streamTypes.all { st -> st in es.registeredTypes } }
        } ?: throw IllegalStateException("$eventListener has a stream type which needs to be registered in one (and only one) event store.")
        return scope.launchListener(eventListener, eventStore, bookmark, observer, template, config.errorBackoff, config.batchSize) { stopped }
    }

    private fun startBatchJob(eventListener: BatchEventListener): Job {
        @Suppress("UNCHECKED_CAST")
        val eventStore = when (eventListener) {
            is SingleStreamTypeBatchEventListener<*, *> ->
                eventStores.singleOrNull {
                    (eventListener as SingleStreamTypeBatchEventListener<Any, Any>).streamType in it.registeredTypes
                }
            is MultiStreamTypeBatchEventListener<*, *> ->
                eventStores.singleOrNull { es ->
                    (eventListener as MultiStreamTypeBatchEventListener<Any, Any>).streamTypes.all { st -> st in es.registeredTypes }
                }
        } ?: throw IllegalStateException("$eventListener has a stream type which needs to be registered in one (and only one) event store.")
        return scope.launchBatchListener(
            eventListener,
            eventStore,
            bookmark,
            observer,
            template,
            config.errorBackoff,
            config.batchSize,
            config.writeBatchSize,
            config.batchTimeout,
        ) {
            stopped
        }
    }

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
        val eventListener = (singleEventListeners + batchEventListeners).single { it.id == id }
        jobs[eventListener.id] = when (eventListener) {
            is SingleEventListener -> startJob(eventListener)
            is BatchEventListener -> startBatchJob(eventListener)
        }
    }

    public suspend fun restartEventListener(id: String) {
        stopEventListener(id).join()
        startEventListener(id)
    }
}
