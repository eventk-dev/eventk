package dev.eventk.store.impl.memory

import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.LoadStreamTest

internal class SynchronizedInMemoryLoadStreamTest : LoadStreamTest<Storage, InMemoryEventStore, InMemoryStreamTestFactory>(
    InMemoryStreamTestFactory(StorageImpl.Synchronized),
)
