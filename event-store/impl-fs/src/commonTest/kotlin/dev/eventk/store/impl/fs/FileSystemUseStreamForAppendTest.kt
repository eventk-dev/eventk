package dev.eventk.store.impl.fs

import dev.eventk.store.test.UseStreamForAppendTest

internal class FileSystemUseStreamForAppendTest : UseStreamForAppendTest<FileSystemStorage, FileSystemEventStore, FileSystemStreamTestFactory>(
    FileSystemStreamTestFactory(),
)
