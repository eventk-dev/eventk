package dev.eventk.arch.hex.adapter.common

import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.EventListener
import dev.eventk.arch.hex.port.MultiStreamTypeEventListener
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType
import dev.eventk.store.impl.memory.InMemoryEventStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class StartJobTest {
    private object TestStreamType : StreamType<String, String> {
        override val id = "test"
    }

    private class InMemoryBookmark : Bookmark {
        private val positions = mutableMapOf<String, Long>()
        override fun get(id: String) = positions[id] ?: 0L
        override fun set(id: String, value: Long) {
            positions[id] = value
        }
    }

    private val noopObserver = object : Observer {
        override fun started(eventListener: EventListener) = Unit
        override fun finished(eventListener: EventListener) = Unit
        override fun envelopeCompleted(eventListener: EventListener, envelope: EventEnvelope<Any, Any>) = Unit
        override fun envelopeFailed(eventListener: EventListener, envelope: EventEnvelope<Any, Any>, t: Throwable, backoff: Duration) = Unit
        override fun failed(eventListener: EventListener, t: Throwable, backoff: Duration) = Unit
    }

    @Test
    fun `single stream type listener receives appended events`() = runTest(timeout = 5.seconds) {
        // given
        val received = CompletableDeferred<EventEnvelope<String, String>>()
        val listener = object : SingleStreamTypeEventListener<String, String> {
            override val id = "test-listener"
            override val streamType: StreamType<String, String> = TestStreamType
            override fun listen(envelope: EventEnvelope<String, String>) {
                received.complete(envelope)
            }
        }
        val store = InMemoryEventStore { registerStreamType(TestStreamType) }
        store.withStreamType(TestStreamType).appendStream("stream-1", 0, listOf("hello"))

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launchListener(
            eventListener = listener,
            eventStore = store,
            bookmark = InMemoryBookmark(),
            observer = noopObserver,
            template = EventBatchTemplate.NoOp(),
            errorBackoff = BackoffStrategy.Constant(5.seconds),
            readBatchSize = 10,
        ) { false }

        // when
        advanceTimeBy(1.seconds)
        val envelope = received.await()

        // then
        assertEquals("hello", envelope.event)
    }

    @Test
    fun `single stream type listener fails`() = runTest(timeout = 5.seconds) {
        // given
        val failedEnvelopeFuture = CompletableDeferred<EventEnvelope<Any, Any>>()
        val listener = object : SingleStreamTypeEventListener<String, String> {
            override val id = "test-listener"
            override val streamType: StreamType<String, String> = TestStreamType
            override fun listen(envelope: EventEnvelope<String, String>) {
                throw RuntimeException("test")
            }
        }
        val store = InMemoryEventStore { registerStreamType(TestStreamType) }
        store.withStreamType(TestStreamType).appendStream("stream-1", 0, listOf("hello"))

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launchListener(
            eventListener = listener,
            eventStore = store,
            bookmark = InMemoryBookmark(),
            observer = object : Observer {
                override fun started(eventListener: EventListener) = Unit
                override fun finished(eventListener: EventListener) = Unit
                override fun envelopeCompleted(eventListener: EventListener, envelope: EventEnvelope<Any, Any>) = Unit
                override fun envelopeFailed(eventListener: EventListener, envelope: EventEnvelope<Any, Any>, t: Throwable, backoff: Duration) {
                    failedEnvelopeFuture.complete(envelope)
                }

                override fun failed(eventListener: EventListener, t: Throwable, backoff: Duration) = Unit
            },
            template = EventBatchTemplate.NoOp(),
            errorBackoff = BackoffStrategy.Constant(5.seconds),
            readBatchSize = 10,
        ) { false }

        // when
        advanceTimeBy(1.seconds)
        val failedEnvelope = failedEnvelopeFuture.await()

        // then
        assertEquals("stream-1", failedEnvelope.streamId)
    }

    @Test
    fun `multi stream type listener receives appended events`() = runTest(timeout = 5.seconds) {
        // given
        val received = CompletableDeferred<EventEnvelope<String, String>>()
        val listener = object : MultiStreamTypeEventListener<String, String> {
            override val id = "multi-listener"
            override val streamTypes = listOf(TestStreamType)
            override fun listen(envelope: EventEnvelope<String, String>) {
                received.complete(envelope)
            }
        }
        val store = InMemoryEventStore { registerStreamType(TestStreamType) }
        store.withStreamType(TestStreamType).appendStream("stream-1", 0, listOf("hello"))

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launchListener(
            eventListener = listener,
            eventStore = store,
            bookmark = InMemoryBookmark(),
            observer = noopObserver,
            template = EventBatchTemplate.NoOp(),
            errorBackoff = BackoffStrategy.Constant(5.seconds),
            readBatchSize = 10,
        ) { false }

        // when
        advanceTimeBy(1.seconds)
        val envelope = received.await()

        // then
        assertEquals("hello", envelope.event)
    }
}
