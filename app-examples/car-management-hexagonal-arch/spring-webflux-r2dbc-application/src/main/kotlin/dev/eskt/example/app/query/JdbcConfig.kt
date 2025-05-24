package dev.eskt.example.app.query

import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@Configuration
@EnableJdbcRepositories // (basePackages = ["dev.eskt.example.app.query"])
class JdbcConfig {

}
