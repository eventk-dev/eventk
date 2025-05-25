package dev.eventk.store.impl.memory

import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.AppendStreamTest

internal class CopyOnWriteInMemoryAppendStreamTest : AppendStreamTest<Storage, InMemoryEventStore, InMemoryStreamTestFactory>(
    InMemoryStreamTestFactory(StorageImpl.CopyOnWrite),
)
