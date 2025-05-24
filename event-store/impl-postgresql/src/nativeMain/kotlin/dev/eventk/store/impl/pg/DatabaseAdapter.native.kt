package dev.eventk.store.impl.pg

internal class NativeDatabaseAdapter : DatabaseAdapter {
    override fun getEntryByPosition(
        position: Long,
        tableInfo: TableInfo,
    ): DatabaseEntry {
        TODO("Not yet implemented")
    }

    override fun getEntryBatch(
        sincePosition: Long,
        batchSize: Int,
        tableInfo: TableInfo,
    ): List<DatabaseEntry> {
        TODO("Not yet implemented")
    }

    override fun getEntryBatch(
        sincePosition: Long,
        batchSize: Int,
        type: String,
        tableInfo: TableInfo,
    ): List<DatabaseEntry> {
        TODO("Not yet implemented")
    }

    override fun <R> useEntriesByStreamIdAndVersion(
        streamId: String,
        sinceVersion: Int,
        limit: Int,
        tableInfo: TableInfo,
        consume: (Sequence<DatabaseEntry>) -> R,
    ): R {
        TODO("Not yet implemented")
    }

    private fun ResultSet.databaseEntry(): DatabaseEntry {
        TODO("Not yet implemented")
    }

    override fun persistEntries(streamId: String, expectedVersion: Int, entries: List<DatabaseEntry>, tableInfo: TableInfo) {
        TODO("Not yet implemented")
    }
}
