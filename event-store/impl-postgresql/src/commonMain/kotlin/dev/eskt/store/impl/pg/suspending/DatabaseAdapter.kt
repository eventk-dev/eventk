package dev.eskt.store.impl.pg.suspending

import dev.eskt.store.impl.pg.DatabaseEntry
import dev.eskt.store.impl.pg.TableInfo

internal interface DatabaseAdapter {
    suspend fun getEntryByPosition(
        position: Long,
        tableInfo: TableInfo,
    ): DatabaseEntry

    suspend fun getEntryBatch(
        sincePosition: Long,
        batchSize: Int,
        type: String?,
        tableInfo: TableInfo,
    ): List<DatabaseEntry>

    suspend fun <R> useEntriesByStreamIdAndVersion(
        streamId: String,
        sinceVersion: Int,
        limit: Int = Int.MAX_VALUE,
        tableInfo: TableInfo,
        consume: (Sequence<DatabaseEntry>) -> R,
    ): R

    suspend fun persistEntries(
        streamId: String,
        expectedVersion: Int,
        entries: List<DatabaseEntry>,
        tableInfo: TableInfo,
    )
}
