package dev.eventk.arch.hex.adapter.common

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun <T> Flow<T>.chunkedWithTimeout(size: Int, timeout: Duration): Flow<List<T>> {
    require(size >= 1) { "Expected positive chunk size, but got $size" }
    require(timeout >= 10.milliseconds) { "Timeout needs to be greater than 10ms, but got $timeout" }

    return channelFlow {
        val timeoutChannel = Channel<Pair<T?, List<T>?>>(Channel.UNLIMITED)
        var currentChunk: ArrayList<T>? = null
        var flushJob: Job? = null

        val elementFlow = this@chunkedWithTimeout
            .map { Pair<T?, List<T>?>(it, null) }
            .onCompletion { timeoutChannel.close() }
        val timeoutFlow = timeoutChannel.receiveAsFlow()

        merge(elementFlow, timeoutFlow).collect { (element, timeoutChunk) ->
            if (timeoutChunk != null) {
                if (timeoutChunk === currentChunk) {
                    send(timeoutChunk)
                    currentChunk = null
                }
            } else {
                val value = element!! // either value in the pair is always non-null
                val chunk = currentChunk ?: ArrayList<T>(size).also {
                    currentChunk = it
                    flushJob = launch {
                        delay(timeout)
                        timeoutChannel.trySend(Pair(null, it))
                    }
                }
                chunk.add(value)
                if (chunk.size == size) {
                    send(chunk)
                    flushJob?.cancel()
                    currentChunk = null
                }
            }
        }

        currentChunk?.let { remainder -> send(remainder) }
    }
}
