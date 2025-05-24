package dev.eventk.store.impl.memory

import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.LoadStreamTest

internal class CopyOnWriteInMemoryLoadStreamTest : LoadStreamTest<Storage, InMemoryEventStore, InMemoryStreamTestFactory>(
    InMemoryStreamTestFactory(StorageImpl.CopyOnWrite),
)
