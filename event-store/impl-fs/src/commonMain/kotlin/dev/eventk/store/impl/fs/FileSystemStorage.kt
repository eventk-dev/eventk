package dev.eventk.store.impl.fs

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.EventMetadata
import dev.eventk.store.api.Serializer
import dev.eventk.store.api.StreamType
import dev.eventk.store.storage.api.StorageVersionMismatchException
import dev.eventk.store.storage.api.blocking.Storage
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import okio.FileHandle
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

@OptIn(ExperimentalSerializationApi::class)
public class FileSystemStorage internal constructor(
    private val config: FileSystemConfig,
) : Storage {
    internal val basePath: Path

    init {
        if (!fs.exists(config.basePath)) {
            fs.createDirectories(config.basePath)
        }
        basePath = fs.canonicalize(config.basePath)

        // TODO implement data -> index sync check
    }

    private val eventMetadataSerializer = config.eventMetadataSerializer
    private val registeredTypes = config.registeredTypes.associateBy { it.id }

    public companion object {
        internal val fs = eventStoreFileSystem()
        private const val STREAM_ENTRY_SIZE_IN_BYTES: Long = (Long.SIZE_BYTES).toLong()
        private const val POSITION_ENTRY_SIZE_IN_BYTES: Long = (Long.SIZE_BYTES).toLong()
    }

    private val dataPath = basePath / "dat"
    private val posPath = basePath / "pos"

    private val writeLock = reentrantLock()

    internal fun initStorage() {
        if (!fs.exists(dataPath) && !fs.exists(posPath)) {
            fs.sink(dataPath).buffer().use { }
            fs.sink(posPath).buffer().use { }
        }
    }

    override fun <E, I> add(streamType: StreamType<E, I>, streamId: I, expectedVersion: Int, events: List<E>, metadata: EventMetadata) {
        if (streamType.id !in registeredTypes) throwIfNotRegistered(streamType.id)
        val streamPath = basePath / toPathComponent(streamId, streamType.stringIdSerializer)
        fs.createDirectories(streamPath.parent!!)

        // creating payloads in serialized form before acquiring any locks
        val metadataPayload = eventMetadataSerializer.serialize(metadata)
        val dataEntries = events.mapIndexed { index, event ->
            val eventPayload = streamType.binaryEventSerializer.serialize(event)
            val dataFileEntry = DataFileEntry(
                type = streamType.id,
                id = streamType.stringIdSerializer.serialize(streamId),
                version = expectedVersion + index + 1,
                eventPayload = eventPayload,
                metadataPayload = metadataPayload,
            )
            dataFileEntryFormat.encodeToByteArray(DataFileEntry.serializer(), dataFileEntry)
        }

        writeLock.withLock {
            useHandles(fs.openReadWrite(dataPath), fs.openReadWrite(posPath), fs.openReadWrite(streamPath)) { dataHandleRw, posHandleRw, streamHandleRw ->
                if (streamHandleRw.size() != expectedVersion * STREAM_ENTRY_SIZE_IN_BYTES) {
                    throw StorageVersionMismatchException((streamHandleRw.size() / STREAM_ENTRY_SIZE_IN_BYTES).toInt(), expectedVersion)
                }

                val dataAddresses = dataHandleRw.appendingSink().buffer().use { dataBuffer ->
                    // calculate position based on previous data entry
                    val dataAddressFirstAppend = dataHandleRw.size()
                    val positionFirstAppend = 1L + if (dataAddressFirstAppend > 0) {
                        dataHandleRw.source(dataAddressFirstAppend - Long.SIZE_BYTES).use { s ->
                            s.buffer().use { it.readLong() }
                        }
                    } else {
                        0L
                    }

                    var appended = 0L
                    dataEntries.mapIndexed { index, dataEntryBytes ->
                        val addr = dataAddressFirstAppend + appended // address we are about to write right now

                        dataBuffer.writeInt(dataEntryBytes.size)
                        dataBuffer.write(dataEntryBytes)
                        dataBuffer.writeInt(dataEntryBytes.size)
                        dataBuffer.writeLong(positionFirstAppend + index)
                        appended += 4L + dataEntryBytes.size + 4L + STREAM_ENTRY_SIZE_IN_BYTES

                        addr
                    }
                }

                // write position index buffer
                posHandleRw.appendingSink().buffer().use { posBuffer ->
                    dataAddresses.forEach { dataAddress ->
                        posBuffer.writeLong(dataAddress)
                    }
                }

                // write stream index
                streamHandleRw.appendingSink().buffer().use { streamBuffer ->
                    dataAddresses.forEach { walAddress ->
                        streamBuffer.writeLong(walAddress)
                    }
                }
            }
        }
    }

    private inline fun <R> useHandles(data: FileHandle, pos: FileHandle, stream: FileHandle, block: (FileHandle, FileHandle, FileHandle) -> R): R {
        var thrown: Throwable? = null

        val dataSize = data.size()
        val posSize = pos.size()
        val streamSize = stream.size()
        val result = try {
            block(data, pos, stream)
        } catch (t: Throwable) {
            thrown = t
            // on error, trying to reset all files to the original size before the operation
            try {
                data.resize(dataSize)
                data.flush()
            } catch (t: Throwable) {
                thrown.addSuppressed(t)
            }
            try {
                pos.resize(posSize)
                pos.flush()
            } catch (t: Throwable) {
                thrown.addSuppressed(t)
            }
            try {
                stream.resize(streamSize)
                stream.flush()
            } catch (t: Throwable) {
                thrown.addSuppressed(t)
            }
            null
        } finally {
            try {
                data.close()
            } catch (t: Throwable) {
                if (thrown == null) {
                    thrown = t
                } else {
                    thrown.addSuppressed(t)
                }
            }
            try {
                pos.close()
            } catch (t: Throwable) {
                if (thrown == null) {
                    thrown = t
                } else {
                    thrown.addSuppressed(t)
                }
            }
            try {
                stream.close()
            } catch (t: Throwable) {
                if (thrown == null) {
                    thrown = t
                } else {
                    thrown.addSuppressed(t)
                }
            }
        }

        if (thrown != null) throw thrown
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    override fun <E, I, R> useStreamEvents(
        streamType: StreamType<E, I>,
        streamId: I,
        sinceVersion: Int,
        consume: (Sequence<EventEnvelope<E, I>>) -> R,
    ): R {
        if (streamType.id !in registeredTypes) throwIfNotRegistered(streamType.id)
        val streamPath = basePath / toPathComponent(streamId, streamType.stringIdSerializer)

        if (!fs.exists(streamPath)) {
            return consume(emptySequence())
        }

        fs.openReadOnly(streamPath).use { streamHandle ->
            streamHandle.source(sinceVersion * STREAM_ENTRY_SIZE_IN_BYTES).buffer().use { streamSource ->
                fs.openReadOnly(dataPath).use { walHandle ->
                    return consume(
                        sequence {
                            var read = 0
                            while (!streamSource.exhausted()) {
                                val addr = streamSource.readLong()
                                read++
                                val envelope = try {
                                    walHandle.readEventEnvelopeAt<E, I>(addr, streamTypeFinder = { id -> registeredTypes[id].asTyped() })
                                } catch (e: okio.IOException) {
                                    val message = "Error reading data file at address $addr, pointer from stream $streamId version ${sinceVersion + read}."
                                    throw CorruptedDataException(message, e)
                                }
                                yield(envelope)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int): List<EventEnvelope<E, I>> {
        return loadEventBatchInternal(sincePosition, batchSize, null)
    }

    override fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int, streamType: StreamType<E, I>): List<EventEnvelope<E, I>> {
        return loadEventBatchInternal(sincePosition, batchSize, streamType)
    }

    private fun <E, I> loadEventBatchInternal(sincePosition: Long, batchSize: Int, streamType: StreamType<E, I>?): List<EventEnvelope<E, I>> {
        if (!fs.exists(posPath)) {
            throw IllegalStateException("Event store is empty, file $posPath does not exist")
        }

        fs.openReadOnly(posPath).use { posHandle ->
            posHandle.source(sincePosition * POSITION_ENTRY_SIZE_IN_BYTES).buffer().use { posSource ->
                fs.openReadOnly(dataPath).use { walHandle ->
                    var read = 0
                    var added = 0
                    return buildList {
                        while (!posSource.exhausted()) {
                            val addr = posSource.readLong()
                            read++
                            val expectedPosition = sincePosition + read
                            val envelope = try {
                                walHandle.readEventEnvelopeAt<E, I>(addr, streamTypeFinder = { id -> registeredTypes[id].asTyped() })
                            } catch (e: okio.IOException) {
                                throw CorruptedDataException("Error reading data file at address $addr, pointer from position $expectedPosition.", e)
                            }
                            if (expectedPosition != envelope.position) {
                                throw CorruptedDataException("Event store is corrupted, expected position $expectedPosition, but entry is ${envelope.position}")
                            }
                            if (streamType == null || streamType == envelope.streamType) {
                                add(envelope)
                                added++
                                if (added == batchSize) break
                            }
                        }
                    }
                }
            }
        }
    }

    private fun throwIfNotRegistered(streamTypeId: String): Nothing {
        throw IllegalStateException("Unregistered type: $streamTypeId")
    }

    private fun <E, I> FileHandle.readEventEnvelopeAt(
        addr: Long,
        streamTypeFinder: (typeId: String) -> StreamType<E, I>,
    ): EventEnvelope<E, I> = source(addr).buffer().use { walBuffer ->
        val entrySize = walBuffer.readInt()
        val walEntryByteArray = walBuffer.readByteArray(entrySize.toLong())
        walBuffer.skip(4) // ignore second copy of the entry size
        val position = walBuffer.readLong()

        val dataFileEntry = dataFileEntryFormat.decodeFromByteArray(DataFileEntry.serializer(), walEntryByteArray)
        val streamType = streamTypeFinder(dataFileEntry.type)

        return EventEnvelope(
            streamType,
            streamType.stringIdSerializer.deserialize(dataFileEntry.id),
            dataFileEntry.version,
            position,
            eventMetadataSerializer.deserialize(dataFileEntry.metadataPayload),
            streamType.binaryEventSerializer.deserialize(dataFileEntry.eventPayload),
        )
    }

    private fun <I> toPathComponent(streamId: I, serializer: Serializer<I, String>): Path {
        return serializer.serialize(streamId).split('/').fold("streams".toPath()) { current, next -> current / next }
    }

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    private inline fun <E, I> StreamType<*, *>?.asTyped(): StreamType<E, I> {
        return this as? StreamType<E, I> ?: throwIfNotRegistered(this?.id ?: "")
    }

    @Suppress("UNCHECKED_CAST")
    private val <E, I> StreamType<E, I>.stringIdSerializer: Serializer<I, String>
        get() = config.idSerializers[this] as Serializer<I, String>

    @Suppress("UNCHECKED_CAST")
    private val <E, I> StreamType<E, I>.binaryEventSerializer: Serializer<E, ByteArray>
        get() = config.payloadSerializers[this] as Serializer<E, ByteArray>
}
