package dev.eventk.store.impl.fs

import okio.FileSystem

internal actual fun eventStoreFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}
