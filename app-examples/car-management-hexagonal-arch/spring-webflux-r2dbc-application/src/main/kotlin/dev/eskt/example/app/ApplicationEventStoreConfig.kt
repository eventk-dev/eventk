package dev.eskt.example.app

import dev.eskt.arch.hex.adapter.common.BackoffStrategy
import dev.eskt.arch.hex.adapter.common.EventBatchTemplate
import dev.eskt.arch.hex.adapter.common.EventListenerExecutorConfig
import dev.eskt.arch.hex.port.EventListener
import dev.eskt.store.api.suspending.EventStore
import dev.eskt.store.impl.pg.PostgresqlEventStore
import dev.eskt.store.test.w.car.CarStreamType
import dev.eskt.store.test.w.driver.DriverStreamType
import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import kotlin.time.Duration.Companion.seconds

@Configuration
//@ComponentScan(
//    basePackages = [
//        "dev.eskt.arch.hex.adapter.spring",
//    ],
//)
class ApplicationEventStoreConfig {
    @Bean
    fun eventStore(connectionFactory: ConnectionFactory): EventStore {
//        val transactionAwareDataSource = TransactionAwareDataSourceProxy(dataSource)
        return PostgresqlEventStore(connectionFactory, "event") {
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
//@Component
//class TransactionalEventBatchTemplate(
//    private val transactionTemplate: TransactionTemplate,
//) : EventBatchTemplate {
//    override fun execute(eventStore: EventStore, eventListener: EventListener, block: () -> Unit) {
//        transactionTemplate.execute {
//            block()
//        }
//    }
//}
