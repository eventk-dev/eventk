package dev.eventk.store.impl.pg

import com.zaxxer.hikari.HikariConfig

internal fun ConnectionConfig.toHikariConfig(): HikariConfig = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://${this@toHikariConfig.host}:${this@toHikariConfig.port}/${this@toHikariConfig.database}"
    schema = this@toHikariConfig.schema
    username = this@toHikariConfig.user
    password = this@toHikariConfig.pass
    minimumIdle = this@toHikariConfig.minPoolSize
    maximumPoolSize = this@toHikariConfig.maxPoolSize
}
