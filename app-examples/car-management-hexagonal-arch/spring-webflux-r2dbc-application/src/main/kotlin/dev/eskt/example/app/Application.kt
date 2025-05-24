package dev.eskt.example.app

import io.r2dbc.spi.ConnectionFactory
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.relational.core.mapping.RelationalMappingContext
import javax.sql.DataSource


@SpringBootApplication
@EnableR2dbcRepositories(basePackages = ["dev.eskt.example.app.command"])
class Application {
    @Bean
    fun mappingContext(): RelationalMappingContext {
        return RelationalMappingContext()
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    fun dataSourceProperties(): DataSourceProperties {
        return DataSourceProperties()
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    fun dataSource(): DataSource {
        return dataSourceProperties().initializeDataSourceBuilder().build()
    }

//    @Bean
//    @ConfigurationProperties(prefix = "spring.r2dbc")
//    fun r2dbcProperties(): R2dbcProperties {
//        return R2dbcProperties()
//    }

    @Bean
    fun connectionFactory(r2dbcProperties: R2dbcProperties): ConnectionFactory {
//        val r2dbcProperties = r2dbcProperties()
        val url = r2dbcProperties.url ?: throw IllegalStateException("R2DBC URL must be provided.")
        return ConnectionFactoryBuilder.withUrl(url)
            .username(r2dbcProperties.username)
            .password(r2dbcProperties.password)
            .build()
    }

    @Bean
    fun r2dbcEntityTemplate(connectionFactory: ConnectionFactory): R2dbcEntityTemplate {
        return R2dbcEntityTemplate(connectionFactory)
    }

    @Bean
    fun eventStoreSchemaMigrationInitializer(datasource: DataSource): FlywayMigrationInitializer {
        val flywayConfig = FluentConfiguration()
            .dataSource(datasource)
            .locations("classpath:/db/migration")
            .ignoreMigrationPatterns("")
            .defaultSchema("public")
            .createSchemas(true)

        return FlywayMigrationInitializer(flywayConfig.load()) {
            it.migrate()
        }
    }

//
//    @Bean
//    fun namedParameterJdbcOperations(dataSource: DataSource): NamedParameterJdbcOperations {
//        return NamedParameterJdbcTemplate(dataSource)
//    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
