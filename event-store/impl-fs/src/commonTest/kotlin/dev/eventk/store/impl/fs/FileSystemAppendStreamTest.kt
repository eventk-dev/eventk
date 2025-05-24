package dev.eventk.store.impl.fs

import dev.eventk.store.test.AppendStreamTest

internal class FileSystemAppendStreamTest : AppendStreamTest<FileSystemStorage, FileSystemEventStore, FileSystemStreamTestFactory>(
    FileSystemStreamTestFactory(),
)
