package dev.eventk.store.test

import dev.eventk.store.api.EventStore
import dev.eventk.store.storage.api.blocking.Storage

@OptIn(ExperimentalStdlibApi::class)
public abstract class StreamTestFactory<R : Storage, S : EventStore> {
    public val closeables: MutableList<AutoCloseable> = mutableListOf()

    public fun closeAll() {
        closeables.forEach { it.close() }
        closeables.clear()
    }

    protected abstract fun createStorage(): R

    public fun newStorage(): R {
        return createStorage()
    }

    protected abstract fun createEventStore(storage: R): S

    public fun newEventStore(storage: R): S {
        return createEventStore(storage)
    }
}
