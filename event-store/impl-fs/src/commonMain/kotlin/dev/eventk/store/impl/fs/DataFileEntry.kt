package dev.eventk.store.impl.fs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal class DataFileEntry(
    @ProtoNumber(1)
    val type: String,
    @ProtoNumber(2)
    val id: String,
    @ProtoNumber(3)
    val version: Int,
    @ProtoNumber(4)
    val eventPayload: ByteArray,
    @ProtoNumber(5)
    val metadataPayload: ByteArray,
)
