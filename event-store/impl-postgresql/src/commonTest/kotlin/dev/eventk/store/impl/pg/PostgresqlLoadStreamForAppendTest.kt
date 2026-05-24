package dev.eventk.store.impl.pg

import dev.eventk.store.test.LoadStreamForAppendTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

internal class PostgresqlLoadStreamForAppendTest : LoadStreamForAppendTest<PostgresqlJdbcStorage, PostgresqlJdbcEventStore, PostgresqlStreamTestFactory>(
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
