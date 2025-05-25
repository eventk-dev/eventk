package dev.eventk.arch.hex.adapter.common

public data class EventListenerExecutorConfig(
    public val threadPoolName: String = "evt-listener",
    public val threadPoolSize: Int = 4,
    public val batchSize: Int = 100,
    public val errorBackoff: BackoffStrategy = BackoffStrategy.Exponential(),
)
