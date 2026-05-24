package dev.eventk.store.impl.pg

import dev.eventk.store.storage.api.StorageVersionMismatchException
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import java.sql.BatchUpdateException
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

internal class JdbcDatabaseAdapter(
    private val dataSource: DataSource,
) : dev.eventk.store.impl.pg.blocking.DatabaseAdapter {
    override fun getEntryByPosition(
        position: Long,
        tableInfo: TableInfo,
    ): DatabaseEntry {
        dataSource.connection.use { connection ->
            connection.prepareStatement(selectEventByPositionSql(tableInfo))
                .use { ps ->
                    ps.setLong(1, position)
                    ps.executeQuery().use { rs ->
                        rs.next()
                        return rs.databaseEntry()
                    }
                }
        }
    }

    override fun getEntryBatch(
        sincePosition: Long,
        batchSize: Int,
        type: String?,
        tableInfo: TableInfo,
    ): List<DatabaseEntry> {
        dataSource.connection.use { connection ->
            when (type) {
                null -> connection.prepareStatement(selectEventSincePositionSql(tableInfo)).use { ps ->
                    ps.setLong(1, sincePosition)
                    ps.setInt(2, batchSize)
                    ps.executeQuery().use { rs ->
                        return buildList {
                            while (rs.next()) {
                                add(rs.databaseEntry())
                            }
                        }
                    }
                }

                else -> connection.prepareStatement(selectEventByTypeSincePositionSql(tableInfo)).use { ps ->
                    ps.setObject(1, type, java.sql.Types.OTHER)
                    ps.setLong(2, sincePosition)
                    ps.setInt(3, batchSize)
                    ps.executeQuery().use { rs ->
                        return buildList {
                            while (rs.next()) {
                                add(rs.databaseEntry())
                            }
                        }
                    }
                }
            }
        }
    }

    override fun <R> useEntriesByStreamIdAndVersion(
        streamId: String,
        sinceVersion: Int,
        limit: Int,
        tableInfo: TableInfo,
        consume: (Sequence<DatabaseEntry>) -> R,
    ): R {
        dataSource.connection.use { connection ->
            val stm = selectEventByStreamIdAndVersionSql(tableInfo)
            connection.prepareStatement(stm).use { ps ->
                ps.setObject(1, streamId, java.sql.Types.OTHER)
                ps.setInt(2, sinceVersion)
                ps.setInt(3, limit)
                ps.executeQuery().use { rs ->
                    return consume(
                        sequence {
                            while (rs.next()) {
                                yield(rs.databaseEntry())
                            }
                        },
                    )
                }
            }
        }
    }

    private fun ResultSet.databaseEntry(): DatabaseEntry {
        return DatabaseEntry(
            position = getLong("position"),
            type = getString("stream_type"),
            id = getString("stream_id"),
            version = getInt("version"),
            eventPayload = getString("payload"),
            metadataPayload = getString("metadata"),
        )
    }

    override fun <R> useEntriesByStreamIdAndVersionWithLock(
        streamId: String,
        sinceVersion: Int,
        tableInfo: TableInfo,
        block: (loaded: Sequence<DatabaseEntry>, persist: (entries: List<DatabaseEntry>, expectedVersion: Int) -> List<DatabaseEntry>) -> R,
    ): R {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement("select pg_advisory_xact_lock(hashtextextended(?, 0))").use { ps ->
                    ps.setString(1, streamId)
                    ps.execute()
                }

                val persist: (List<DatabaseEntry>, expectedVersion: Int) -> List<DatabaseEntry> = { entries, expectedVersion ->
                    if (entries.isEmpty()) {
                        emptyList()
                    } else {
                        connection.prepareStatement(insertEventSql(tableInfo), Statement.RETURN_GENERATED_KEYS).use { ps ->
                            entries.forEach { entry ->
                                ps.setObject(1, entry.type, java.sql.Types.OTHER)
                                ps.setObject(2, entry.id, java.sql.Types.OTHER)
                                ps.setInt(3, entry.version)
                                ps.setObject(4, entry.eventPayload, java.sql.Types.OTHER)
                                ps.setObject(5, entry.metadataPayload, java.sql.Types.OTHER)
                                ps.addBatch()
                            }
                            ps.executeBatchWithExceptionHandling(expectedVersion)

                            val positions = ps.generatedKeys.use { gk ->
                                buildList {
                                    while (gk.next()) {
                                        add(gk.getLong("position"))
                                    }
                                }
                            }
                            entries.mapIndexed { i, entry -> entry.copy(position = positions[i]) }
                        }
                    }
                }

                val result = connection.prepareStatement(selectEventByStreamIdAndVersionSql(tableInfo)).use { ps ->
                    ps.setObject(1, streamId, java.sql.Types.OTHER)
                    ps.setInt(2, sinceVersion)
                    ps.setInt(3, Int.MAX_VALUE)
                    ps.executeQuery().use { rs ->
                        val sequence = sequence {
                            while (rs.next()) {
                                yield(rs.databaseEntry())
                            }
                        }
                        block(sequence, persist)
                    }
                }
                connection.commit()
                return result
            } catch (t: Throwable) {
                try {
                    connection.rollback()
                } catch (rollbackError: Throwable) {
                    t.addSuppressed(rollbackError)
                }
                throw t
            }
        }
    }

    override fun persistEntries(streamId: String, expectedVersion: Int, entries: List<DatabaseEntry>, tableInfo: TableInfo) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(selectMaxVersionByStreamIdSql(tableInfo)).use { ps ->
                ps.setObject(1, streamId, java.sql.Types.OTHER)
                ps.executeQuery().use { rs ->
                    rs.next()
                    val currentVersion = rs.getInt(1)
                    if (expectedVersion != currentVersion) {
                        throw StorageVersionMismatchException(
                            currentVersion,
                            expectedVersion,
                        )
                    }
                }
            }

            if (entries.isEmpty()) return@use

            connection.prepareStatement(insertEventSql(tableInfo)).use { ps ->
                entries.forEach { entry ->
                    ps.setObject(1, entry.type, java.sql.Types.OTHER)
                    ps.setObject(2, entry.id, java.sql.Types.OTHER)
                    ps.setInt(3, entry.version)
                    ps.setObject(4, entry.eventPayload, java.sql.Types.OTHER)
                    ps.setObject(5, entry.metadataPayload, java.sql.Types.OTHER)
                    ps.addBatch()
                }
                ps.executeBatchWithExceptionHandling(expectedVersion)
            }
        }
    }

    private fun PreparedStatement.executeBatchWithExceptionHandling(expectedVersion: Int): IntArray? = try {
        this.executeBatch()
    } catch (e: BatchUpdateException) {
        when (val cause = e.cause) {
            is PSQLException -> {
                if (cause.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                    cause.serverErrorMessage
                        ?.let {
                            throw StorageVersionMismatchException(
                                // if expectedVersion already exists, assuming it's at least 1 version ahead
                                expectedVersion + 1,
                                expectedVersion,
                            )
                        }
                        ?: throw e
                } else {
                    throw e
                }
            }

            else -> throw e
        }
    }
}
