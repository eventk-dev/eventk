package dev.eskt.store.impl.pg

import dev.eskt.store.impl.pg.suspending.DatabaseAdapter
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Row
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class R2dbcDatabaseAdapter(
    private val connectionFactory: ConnectionFactory,
) : DatabaseAdapter {
    override suspend fun getEntryByPosition(
        position: Long,
        tableInfo: TableInfo,
    ): DatabaseEntry {
        connectionFactory.create().awaitSingle().use { connection ->
            val result = connection.createStatement(selectEventByPositionSql(tableInfo))
                .bind("$1", position)
                .execute()
                .awaitSingle()

            result
                .map { row: Row, _ -> row }
                .asFlow()
                .collect {
                    println("Updated $it")
                }
        }

        TODO()
//        dataSource.connection.use { connection ->
//            connection.prepareStatement(selectEventByPositionSql(tableInfo))
//                .use { ps ->
//                    ps.setLong(1, position)
//                    ps.executeQuery().use { rs ->
//                        rs.next()
//                        return rs.databaseEntry()
//                    }
//                }
//        }
    }

    override suspend fun getEntryBatch(
        sincePosition: Long,
        batchSize: Int,
        type: String?,
        tableInfo: TableInfo,
    ): List<DatabaseEntry> {
        TODO()
//        dataSource.connection.use { connection ->
//            when (type) {
//                null -> connection.prepareStatement(selectEventSincePositionSql(tableInfo)).use { ps ->
//                    ps.setLong(1, sincePosition)
//                    ps.setInt(2, batchSize)
//                    ps.executeQuery().use { rs ->
//                        return buildList {
//                            while (rs.next()) {
//                                add(rs.databaseEntry())
//                            }
//                        }
//                    }
//                }
//
//                else -> connection.prepareStatement(selectEventByTypeSincePositionSql(tableInfo)).use { ps ->
//                    ps.setObject(1, type, Types.OTHER)
//                    ps.setLong(2, sincePosition)
//                    ps.setInt(3, batchSize)
//                    ps.executeQuery().use { rs ->
//                        return buildList {
//                            while (rs.next()) {
//                                add(rs.databaseEntry())
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    override suspend fun <R> useEntriesByStreamIdAndVersion(
        streamId: String,
        sinceVersion: Int,
        limit: Int,
        tableInfo: TableInfo,
        consume: (Sequence<DatabaseEntry>) -> R,
    ): R {
        TODO()
//        dataSource.connection.use { connection ->
//            val stm = selectEventByStreamIdAndVersionSql(tableInfo)
//            connection.prepareStatement(stm).use { ps ->
//                ps.setObject(1, streamId, Types.OTHER)
//                ps.setInt(2, sinceVersion)
//                ps.setInt(3, limit)
//                ps.executeQuery().use { rs ->
//                    return consume(
//                        sequence {
//                            while (rs.next()) {
//                                yield(rs.databaseEntry())
//                            }
//                        },
//                    )
//                }
//            }
//        }
    }

//    private fun ResultSet.databaseEntry(): DatabaseEntry {
//        return DatabaseEntry(
//            position = getLong("position"),
//            type = getString("stream_type"),
//            id = getString("stream_id"),
//            version = getInt("version"),
//            eventPayload = getString("payload"),
//            metadataPayload = getString("metadata"),
//        )
//    }

    override suspend fun persistEntries(streamId: String, expectedVersion: Int, entries: List<DatabaseEntry>, tableInfo: TableInfo) {
        TODO()
//        dataSource.connection.use { connection ->
//            connection.prepareStatement(selectMaxVersionByStreamIdSql(tableInfo)).use { ps ->
//                ps.setObject(1, streamId, Types.OTHER)
//                ps.executeQuery().use { rs ->
//                    rs.next()
//                    val currentVersion = rs.getInt(1)
//                    if (expectedVersion != currentVersion) {
//                        throw StorageVersionMismatchException(
//                            currentVersion,
//                            expectedVersion,
//                        )
//                    }
//                }
//            }
//
//            if (entries.isEmpty()) return@use
//
//            connection.prepareStatement(insertEventSql(tableInfo)).use { ps ->
//                entries.forEach { entry ->
//                    ps.setObject(1, entry.type, Types.OTHER)
//                    ps.setObject(2, entry.id, Types.OTHER)
//                    ps.setInt(3, entry.version)
//                    ps.setObject(4, entry.eventPayload, Types.OTHER)
//                    ps.setObject(5, entry.metadataPayload, Types.OTHER)
//                    ps.addBatch()
//                }
//                try {
//                    ps.executeBatch()
//                } catch (e: BatchUpdateException) {
//                    when (val cause = e.cause) {
//                        is PSQLException -> {
//                            if (cause.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
//                                cause.serverErrorMessage
//                                    ?.let {
//                                        throw StorageVersionMismatchException(
//                                            // if expectedVersion already exists, assuming it's at least 1 version ahead
//                                            expectedVersion + 1,
//                                            expectedVersion,
//                                        )
//                                    }
//                                    ?: throw e
//                            } else {
//                                throw e
//                            }
//                        }
//
//                        else -> throw e
//                    }
//                }
//            }
//        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun <T : io.r2dbc.spi.Closeable?, R> T.use(block: (T) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        var exception: Throwable? = null
        try {
            return block(this)
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            this.closeFinally(exception)
        }
    }

    internal fun io.r2dbc.spi.Closeable?.closeFinally(cause: Throwable?) = when {
        this == null -> {}
        cause == null -> close()
        else ->
            try {
                close()
            } catch (closeException: Throwable) {
                cause.addSuppressed(closeException)
            }
    }
}
