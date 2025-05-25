package dev.eventk.store.impl.memory

import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.StreamTestFactory
import dev.eventk.store.test.w.car.CarStreamType
import dev.eventk.store.test.w.driver.DriverStreamType

internal class InMemoryStreamTestFactory(val storageImpl: StorageImpl) : StreamTestFactory<Storage, InMemoryEventStore>() {
    private val config = InMemoryConfig(
        registeredTypes = listOf(
            CarStreamType,
            DriverStreamType,
        ),
    )

    override fun createStorage(): Storage {
        return when (storageImpl) {
            StorageImpl.CopyOnWrite -> CopyOnWriteInMemoryStorage(config)
            StorageImpl.Synchronized -> SynchronizedInMemoryStorage(config)
        }
    }

    override fun createEventStore(storage: Storage): InMemoryEventStore {
        return InMemoryEventStore(config, storageImpl, storage)
    }
}
