package dev.eventk.arch.hex.adapter.common

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

public fun interface BackoffStrategy {
    /**
     * Provides the backoff duration before the given n-th retry.
     * The [retry] parameter represents the retry attempt and is guaranteed to be >= 1,
     * where the first retry corresponds to retry = 1, the second retry to retry = 2, and so on.
     */
    public fun backoff(retry: Int): Duration

    public data class Constant(val duration: Duration) : BackoffStrategy {
        init {
            check(duration.isPositive()) // enforcing a non-zero value here since zero will cause busy-waiting
        }

        override fun backoff(retry: Int): Duration = duration
    }

    public data class Exponential(val base: Double = 2.0, val maximumDelay: Duration = 5.minutes) : BackoffStrategy {
        init {
            check(base > 0)
            check(maximumDelay.isPositive())
        }

        override fun backoff(retry: Int): Duration = minOf(base.pow(retry).seconds, maximumDelay)
    }
}
