package dev.eventk.store.impl.pg

public actual typealias DataSource = javax.sql.DataSource

internal actual fun DataSource.createAdapter(): dev.eventk.store.impl.pg.blocking.DatabaseAdapter = JdbcDatabaseAdapter(this)
