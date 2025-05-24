package dev.eventk.store.impl.fs

import okio.FileSystem

internal expect fun eventStoreFileSystem(): FileSystem
