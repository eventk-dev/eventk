package dev.eventk.store.impl.pg

import dev.eventk.store.test.LoadAndAppendStreamTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

internal class PostgresqlLoadAndAppendStreamTest : LoadAndAppendStreamTest<PostgresqlJdbcStorage, PostgresqlJdbcEventStore, PostgresqlStreamTestFactory>(
    PostgresqlStreamTestFactory(),
) {
    @BeforeTest
    fun beforeEach() {
        factory.connectionConfig.create("event")
    }

    @AfterTest
    fun afterEach() {
        factory.closeAll()
        factory.connectionConfig.drop()
    }
}
