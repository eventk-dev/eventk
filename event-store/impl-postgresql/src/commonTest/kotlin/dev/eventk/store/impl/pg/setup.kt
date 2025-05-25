package dev.eventk.store.impl.pg

@OptIn(ExperimentalStdlibApi::class)
internal expect fun ConnectionConfig.dataSource(closeables: MutableList<AutoCloseable>): DataSource

internal expect fun ConnectionConfig.create(eventTable: String, streamIdType: String = "uuid")

internal expect fun ConnectionConfig.drop()
