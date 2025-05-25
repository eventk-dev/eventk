package dev.eventk.example.domain

interface UnitOfWork {
    fun <T> mark(action: () -> T): T
}
