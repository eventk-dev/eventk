package dev.eskt.store.impl.pg

public expect interface ReactiveDataSource

internal expect fun ReactiveDataSource.createAdapter(): dev.eskt.store.impl.pg.suspending.DatabaseAdapter

@Suppress("FunctionName")
public fun PostgresqlEventStore(
    dataSource: ReactiveDataSource,
    eventTable: String,
    eventWriteTable: String = eventTable,
    block: PostgresqlConfigBuilder<ReactiveDataSource>.() -> Unit,
): PostgresqlR2dbcEventStore = PostgresqlR2dbcEventStore(
    PostgresqlConfigBuilder(dataSource, eventTable, eventWriteTable)
        .apply(block)
        .build(),
)
