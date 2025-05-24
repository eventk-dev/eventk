package dev.eskt.example.app

import dev.eskt.example.domain.UnitOfWork
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class UnitOfWorkAdapter(
    val transactionTemplate: TransactionTemplate,
) : UnitOfWork {
    override fun <T> mark(action: () -> T): T = transactionTemplate.execute { action() }!!
}
