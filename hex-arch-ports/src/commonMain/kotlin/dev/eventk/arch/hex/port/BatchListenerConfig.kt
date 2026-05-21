package dev.eventk.arch.hex.port

import kotlin.time.Duration

public data class BatchListenerConfig(
    val readBatchSize: Int? = null,
    val writeBatchSize: Int? = null,
    val batchTimeout: Duration? = null,
)
