package dev.eskt.store.impl.pg

public actual typealias ReactiveDataSource = io.r2dbc.spi.ConnectionFactory

internal actual fun ReactiveDataSource.createAdapter(): dev.eskt.store.impl.pg.suspending.DatabaseAdapter = R2dbcDatabaseAdapter(this)
