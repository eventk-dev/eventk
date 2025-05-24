package dev.eventk.store.impl.fs

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
internal val dataFileEntryFormat: BinaryFormat = ProtoBuf {}
