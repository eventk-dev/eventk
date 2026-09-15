package dev.eventk.store.impl.pg

import dev.eventk.store.test.UseStreamForAppendTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

internal class PostgresqlUseStreamForAppendTest : UseStreamForAppendTest<PostgresqlJdbcStorage, PostgresqlJdbcEventStore, PostgresqlStreamTestFactory>(
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
