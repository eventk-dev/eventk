package dev.eventk.arch.hex.adapter.ktor

import dev.eventk.arch.hex.port.Bookmark
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType
import dev.eventk.store.impl.memory.InMemoryEventStore
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EventListenerExecutorServiceTest {
    private object TestStreamType : StreamType<String, String> {
        override val id = "test"
    }

    private class InMemoryBookmark : Bookmark {
        private val positions = mutableMapOf<String, Long>()
        override fun get(id: String) = positions[id] ?: 0L
        override fun set(id: String, value: Long) { positions[id] = value }
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
            EventListenerExecutorService(
                eventStores = listOf(InMemoryEventStore { registerStreamType(TestStreamType) }),
                bookmark = InMemoryBookmark(),
                singleStreamTypeEventListeners = listOf(listener1, listener2),
                multiStreamTypeEventListeners = emptyList(),
                debugLogger = {},
                infoLogger = {},
                errorLogger = { _, _ -> },
            )
        }
        assertTrue(exception.message!!.contains("same-id"))
    }
}
