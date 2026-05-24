package dev.eventk.store.impl.fs

import dev.eventk.store.test.LoadAndAppendStreamTest

internal class FileSystemLoadAndAppendStreamTest : LoadAndAppendStreamTest<FileSystemStorage, FileSystemEventStore, FileSystemStreamTestFactory>(
    FileSystemStreamTestFactory(),
)
