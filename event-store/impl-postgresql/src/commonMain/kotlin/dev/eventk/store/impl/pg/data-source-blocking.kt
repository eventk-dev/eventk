package dev.eventk.store.impl.pg

public expect interface DataSource

internal expect fun DataSource.createAdapter(): dev.eventk.store.impl.pg.blocking.DatabaseAdapter

@Suppress("FunctionName")
public fun PostgresqlEventStore(
    dataSource: DataSource,
    eventTable: String,
    eventWriteTable: String = eventTable,
    block: PostgresqlConfigBuilder<DataSource>.() -> Unit,
): PostgresqlJdbcEventStore = PostgresqlJdbcEventStore(
    PostgresqlConfigBuilder(dataSource, eventTable, eventWriteTable)
        .apply(block)
        .build(),
)
