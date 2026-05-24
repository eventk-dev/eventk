package dev.eventk.store.impl.memory

import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.LoadAndAppendStreamTest

internal class SynchronizedInMemoryLoadAndAppendStreamTest : LoadAndAppendStreamTest<Storage, InMemoryEventStore, InMemoryStreamTestFactory>(
    InMemoryStreamTestFactory(StorageImpl.Synchronized),
)
