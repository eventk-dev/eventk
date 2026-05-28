package dev.eventk.store.impl.fs

import dev.eventk.store.test.LoadStreamForAppendTest

internal class FileSystemLoadStreamForAppendTest : LoadStreamForAppendTest<FileSystemStorage, FileSystemEventStore, FileSystemStreamTestFactory>(
    FileSystemStreamTestFactory(),
)
