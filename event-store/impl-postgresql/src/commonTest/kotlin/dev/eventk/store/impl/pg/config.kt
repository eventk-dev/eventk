package dev.eventk.store.impl.pg

import kotlin.random.Random

internal fun generateTestConnectionConfig() = ConnectionConfig(
    host = "localhost",
    port = 5432,
    database = "test_${Random.nextInt(0, 2000000000)}",
    schema = "event_store",
    user = "postgres",
    pass = "postgres",
)
