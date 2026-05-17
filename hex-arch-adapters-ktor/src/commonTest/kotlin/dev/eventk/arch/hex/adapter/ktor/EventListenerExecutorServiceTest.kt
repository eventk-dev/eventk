package dev.eventk.arch.hex.adapter.ktor

import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.MultiStreamTypeEventListener
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType
import dev.eventk.store.impl.memory.InMemoryEventStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class EventListenerExecutorServiceTest {

    private object TestStreamType : StreamType<String, String> {
        override val id = "test"
    }

    private class InMemoryBookmark : Bookmark {
        private val positions = mutableMapOf<String, Long>()
        override fun get(id: String) = positions[id] ?: 0L
        override fun set(id: String, value: Long) { positions[id] = value }
    }

    private fun service(
        singleListeners: List<SingleStreamTypeEventListener<*, *>> = emptyList(),
        multiListeners: List<MultiStreamTypeEventListener<*, *>> = emptyList(),
    ) = EventListenerExecutorService(
        eventStores = listOf(InMemoryEventStore { registerStreamType(TestStreamType) }),
        bookmark = InMemoryBookmark(),
        singleStreamTypeEventListeners = singleListeners,
        multiStreamTypeEventListeners = multiListeners,
        debugLogger = {},
        infoLogger = {},
        errorLogger = { _, _ -> },
    )

    @Test
    fun `single stream type listener receives appended events`() = runTest(timeout = 5.seconds) {
        val received = CompletableDeferred<EventEnvelope<String, String>>()
        val listener = object : SingleStreamTypeEventListener<String, String> {
            override val id = "test-listener"
            override val streamType: StreamType<String, String> = TestStreamType
            override fun listen(envelope: EventEnvelope<String, String>) { received.complete(envelope) }
        }
        val store = InMemoryEventStore { registerStreamType(TestStreamType) }
        store.withStreamType(TestStreamType)
            .appendStream("stream-1", 0, listOf("hello"))

        val svc = EventListenerExecutorService(
            eventStores = listOf(store),
            bookmark = InMemoryBookmark(),
            singleStreamTypeEventListeners = listOf(listener),
            multiStreamTypeEventListeners = emptyList(),
            debugLogger = {},
            infoLogger = {},
            errorLogger = { _, _ -> },
        )
        svc.init()

        val envelope = received.await()
        assertEquals("hello", envelope.event)

        svc.shutdown()
    }

    @Test
    fun `multi stream type listener receives appended events`() = runTest(timeout = 5.seconds) {
        val received = CompletableDeferred<EventEnvelope<String, String>>()
        val listener = object : MultiStreamTypeEventListener<String, String> {
            override val id = "multi-listener"
            override val streamTypes = listOf(TestStreamType)
            override fun listen(envelope: EventEnvelope<String, String>) { received.complete(envelope) }
        }
        val store = InMemoryEventStore { registerStreamType(TestStreamType) }
        store.withStreamType(TestStreamType).appendStream("stream-1", 0, listOf("hello"))

        val svc = EventListenerExecutorService(
            eventStores = listOf(store),
            bookmark = InMemoryBookmark(),
            singleStreamTypeEventListeners = emptyList(),
            multiStreamTypeEventListeners = listOf(listener),
            debugLogger = {},
            infoLogger = {},
            errorLogger = { _, _ -> },
        )
        svc.init()

        val envelope = received.await()
        assertEquals("hello", envelope.event)

        svc.shutdown()
    }

    @Test
    fun `duplicate listener ids throw on construction`() {
        val listener1 = object : SingleStreamTypeEventListener<String, String> {
            override val id = "same-id"
            override val streamType: StreamType<String, String> = TestStreamType
            override fun listen(envelope: EventEnvelope<String, String>) = Unit
        }
        val listener2 = object : SingleStreamTypeEventListener<String, String> {
            override val id = "same-id"
            override val streamType: StreamType<String, String> = TestStreamType
            override fun listen(envelope: EventEnvelope<String, String>) = Unit
        }

        val exception = assertFailsWith<IllegalStateException> {
            service(singleListeners = listOf(listener1, listener2))
        }
        assertTrue(exception.message!!.contains("same-id"))
    }
}
