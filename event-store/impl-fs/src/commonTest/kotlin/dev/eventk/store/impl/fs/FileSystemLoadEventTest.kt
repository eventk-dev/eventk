package dev.eventk.store.impl.fs

import dev.eventk.store.test.LoadEventTest

internal class FileSystemLoadEventTest : LoadEventTest<FileSystemStorage, FileSystemEventStore, FileSystemStreamTestFactory>(
    FileSystemStreamTestFactory(),
)
