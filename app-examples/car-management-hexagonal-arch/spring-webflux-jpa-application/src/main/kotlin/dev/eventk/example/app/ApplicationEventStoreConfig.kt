package dev.eventk.example.app

import dev.eventk.arch.hex.adapter.common.BackoffStrategy
import dev.eventk.arch.hex.adapter.common.EventBatchTemplate
import dev.eventk.arch.hex.adapter.common.EventListenerExecutorConfig
import dev.eventk.arch.hex.port.EventListener
import dev.eventk.store.api.blocking.EventStore
import dev.eventk.store.impl.pg.PostgresqlEventStore
import dev.eventk.store.test.w.car.CarStreamType
import dev.eventk.store.test.w.driver.DriverStreamType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

@Configuration
@ComponentScan(
    basePackages = [
        "dev.eventk.arch.hex.adapter.spring",
    ],
)
class ApplicationEventStoreConfig {
    @Bean
    fun eventStore(dataSource: DataSource): EventStore {
        val transactionAwareDataSource = TransactionAwareDataSourceProxy(dataSource)
        return PostgresqlEventStore(transactionAwareDataSource, "event") {
            registerStreamType(CarStreamType)
            registerStreamType(DriverStreamType)
        }
    }

    /**
     * This is how we can provide a customized [EventListenerExecutorConfig] bean instance.
     */
    @Bean
    fun eventListenerExecutorConfig(): EventListenerExecutorConfig {
        return EventListenerExecutorConfig(
            threadPoolName = "custom-event-listener-thread",
            errorBackoff = BackoffStrategy.Constant(30.seconds),
        )
    }
}

/**
 * This is how we can provide a customized [EventBatchTemplate] with transaction support.
 */
@Component
class TransactionalEventBatchTemplate(
    private val transactionTemplate: TransactionTemplate,
) : EventBatchTemplate {
    override fun execute(eventStore: EventStore, eventListener: EventListener, block: () -> Unit) {
        transactionTemplate.execute {
            block()
        }
    }
}
