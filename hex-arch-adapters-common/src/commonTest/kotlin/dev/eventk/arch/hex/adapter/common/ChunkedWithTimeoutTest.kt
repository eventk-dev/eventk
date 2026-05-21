package dev.eventk.arch.hex.adapter.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ChunkedWithTimeoutTest {
    @Test
    fun `given size reached before timeout - when chunkedWithTimeout - then flushes full chunk`() = runTest {
        val source = flow { repeat(6) { emit(it) } }

        val result = source.chunkedWithTimeout(size = 3, timeout = 10.seconds).toList()

        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4, 5)), result)
    }

    @Test
    fun `given timeout fires before size - when chunkedWithTimeout - then flushes partial chunk`() = runTest {
        val source = flow {
            emit(1)
            emit(2)
            delay(200.milliseconds)
        }

        val result = source.chunkedWithTimeout(size = 10, timeout = 100.milliseconds).toList()

        assertEquals(listOf(listOf(1, 2)), result)
    }

    @Test
    fun `given slow elements with gaps between them - when chunkedWithTimeout - then resets timeout after each flush`() = runTest {
        val source = flow {
            emit(1)
            delay(200.milliseconds)
            emit(2)
            delay(200.milliseconds)
        }

        val result = source.chunkedWithTimeout(size = 10, timeout = 100.milliseconds).toList()

        assertEquals(listOf(listOf(1), listOf(2)), result)
    }

    @Test
    fun `given upstream completes before timeout - when chunkedWithTimeout - then drains partial chunk`() = runTest {
        val source = flow {
            emit(1)
            emit(2)
            emit(3)
        }

        val result = source.chunkedWithTimeout(size = 10, timeout = 10.seconds).toList()

        assertEquals(listOf(listOf(1, 2, 3)), result)
    }

    @Test
    fun `given empty upstream - when chunkedWithTimeout - then emits nothing`() = runTest {
        val source = flow<Int> {}

        val result = source.chunkedWithTimeout(size = 5, timeout = 1.seconds).toList()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `given size of zero - when chunkedWithTimeout - then throws`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            flow<Int> {}.chunkedWithTimeout(size = 0, timeout = 1.seconds)
        }
    }

    @Test
    fun `given size of one - when chunkedWithTimeout - then each element is its own batch`() = runTest {
        val source = flow { repeat(3) { emit(it) } }

        val result = source.chunkedWithTimeout(size = 1, timeout = 10.seconds).toList()

        assertEquals(listOf(listOf(0), listOf(1), listOf(2)), result)
    }

    @Test
    fun `given stream completes exactly on chunk boundary - when chunkedWithTimeout - then no empty trailing chunk`() = runTest {
        val source = flow {
            emit(1)
            emit(2)
            emit(3)
        }

        val result = source.chunkedWithTimeout(size = 3, timeout = 10.seconds).toList()

        assertEquals(listOf(listOf(1, 2, 3)), result)
    }

    @Test
    fun `given mixed flush triggers - when chunkedWithTimeout - then handles size limit and timeout and upstream close`() = runTest {
        val source = flow {
            emit(1)
            emit(2)
            emit(3)
            delay(200.milliseconds)
            emit(4)
        }

        val result = source.chunkedWithTimeout(size = 2, timeout = 100.milliseconds).toList()

        assertEquals(listOf(listOf(1, 2), listOf(3), listOf(4)), result)
    }
}
