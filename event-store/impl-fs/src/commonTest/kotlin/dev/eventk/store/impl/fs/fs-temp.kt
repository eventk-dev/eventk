package dev.eventk.store.impl.fs

import okio.FileSystem
import okio.Path
import kotlin.random.Random

internal fun createEmptyTemporaryFolder(): Path {
    val nextInt = Random.nextInt(from = 100000000, until = 1000000000)
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "es-$nextInt"
    FileSystemStorage.fs.deleteRecursively(dir)
    return dir
}
