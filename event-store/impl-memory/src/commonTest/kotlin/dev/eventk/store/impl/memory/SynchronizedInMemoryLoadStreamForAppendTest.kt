package dev.eventk.store.impl.memory

import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.LoadStreamForAppendTest

internal class SynchronizedInMemoryLoadStreamForAppendTest : LoadStreamForAppendTest<Storage, InMemoryEventStore, InMemoryStreamTestFactory>(
    InMemoryStreamTestFactory(StorageImpl.Synchronized),
)
