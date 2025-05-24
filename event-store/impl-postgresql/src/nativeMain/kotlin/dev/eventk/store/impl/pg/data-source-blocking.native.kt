package dev.eventk.store.impl.pg

public actual interface DataSource

internal actual fun DataSource.createAdapter(): DatabaseAdapter = NativeDatabaseAdapter()
