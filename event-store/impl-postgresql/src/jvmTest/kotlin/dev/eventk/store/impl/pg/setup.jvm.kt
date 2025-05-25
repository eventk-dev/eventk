package dev.eventk.store.impl.pg

import com.zaxxer.hikari.HikariDataSource

internal actual fun ConnectionConfig.dataSource(closeables: MutableList<AutoCloseable>): DataSource {
    return HikariDataSource(toHikariConfig()).also { closeables.add(it) }
}

internal actual fun ConnectionConfig.create(eventTable: String, streamIdType: String) {
    val adminConnectionConfig = copy(database = "postgres", minPoolSize = 1, maxPoolSize = 1)
    val adminHikariConfig = adminConnectionConfig.toHikariConfig()
    val adminDataSource = HikariDataSource(adminHikariConfig)

    adminDataSource.connection.use { connection ->
        connection.prepareStatement("create database $database;").use { ps ->
            ps.executeUpdate()
        }
    }

    adminDataSource.close()

    val hikariConfig = toHikariConfig()
    val dataSource = HikariDataSource(hikariConfig)

    dataSource.connection.use { connection ->
        connection.prepareStatement("create schema $schema;").use { ps ->
            ps.executeUpdate()
        }
        connection.prepareStatement(
            """
                CREATE TABLE $schema.$eventTable
                (
                    position    bigserial NOT NULL unique,
                    stream_type text      NOT NULL,
                    stream_id   $streamIdType      NOT NULL,
                    version     int       NOT NULL,
                    payload     jsonb     NOT NULL,
                    metadata    jsonb     NOT NULL,
                    PRIMARY KEY (stream_id, version)
                );
                
                CREATE OR REPLACE FUNCTION acquire_write_lock()
                    RETURNS trigger AS
                    ${'$'}BODY${'$'}
                    BEGIN
                        EXECUTE pg_advisory_xact_lock(1728994214);
                        RETURN null;
                    END;
                    ${'$'}BODY${'$'}
                    LANGUAGE plpgsql;

                CREATE TRIGGER event_acquire_lock_before_insert
                    BEFORE INSERT
                    ON $schema.$eventTable
                    FOR EACH STATEMENT
                EXECUTE PROCEDURE acquire_write_lock();
            """.trimIndent(),
        ).use { ps ->
            ps.executeUpdate()
        }
    }

    dataSource.close()
}

internal actual fun ConnectionConfig.drop() {
    val adminConnectionConfig =
        generateTestConnectionConfig().copy(database = "postgres", minPoolSize = 1, maxPoolSize = 1)
    val adminHikariConfig = adminConnectionConfig.toHikariConfig()
    val adminDataSource = HikariDataSource(adminHikariConfig)

    adminDataSource.connection.use { connection ->
        connection.prepareStatement("drop database $database;").use { ps ->
            ps.executeUpdate()
        }
    }
    adminDataSource.close()
}
