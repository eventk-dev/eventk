package dev.eventk.arch.hex.adapter.common

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public data class EventListenerExecutorConfig(
    public val threadPoolName: String = "evt-listener",
    public val threadPoolSize: Int = 4,
    public val batchSize: Int = 1000,
    public val writeBatchSize: Int = 100,
    public val batchTimeout: Duration = 500.milliseconds,
    public val errorBackoff: BackoffStrategy = BackoffStrategy.Exponential(),
)
