package dev.eventk.store.impl.fs

import okio.FileSystem
import okio.NodeJsFileSystem

internal actual fun eventStoreFileSystem(): FileSystem {
    return NodeJsFileSystem
}
