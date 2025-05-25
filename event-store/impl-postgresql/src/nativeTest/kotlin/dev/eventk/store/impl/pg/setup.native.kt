package dev.eventk.store.impl.pg

@OptIn(ExperimentalStdlibApi::class)
internal actual fun ConnectionConfig.dataSource(closeables: MutableList<AutoCloseable>): DataSource {
    TODO("Not yet implemented")
}

internal actual fun ConnectionConfig.create(eventTable: String) {
    TODO()
}

internal actual fun ConnectionConfig.drop() {
    TODO()
}
