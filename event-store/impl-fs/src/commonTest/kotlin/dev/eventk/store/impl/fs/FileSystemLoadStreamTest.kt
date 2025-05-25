package dev.eventk.store.impl.fs

import com.benasher44.uuid.uuid4
import dev.eventk.store.impl.common.binary.serialization.DefaultEventMetadataSerializer
import dev.eventk.store.test.LoadStreamTest
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarStreamType
import kotlinx.serialization.encodeToByteArray
import okio.buffer
import okio.use
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class FileSystemLoadStreamTest : LoadStreamTest<FileSystemStorage, FileSystemEventStore, FileSystemStreamTestFactory>(
    FileSystemStreamTestFactory(),
) {
    @Test
    @JsName("child_test1")
    fun `given corrupted event 2 - when loading 2 events - then fails`() {
        // given
        val storage = factory.newStorage()
        val dat = storage.basePath / "dat"
        val pos = storage.basePath / "pos"

        val carProduced1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        val carProduced2 = CarProducedEvent(vin = "456", producer = 1, make = "kia", model = "rio")

        FileSystemStorage.fs.sink(dat).buffer().use { d ->
            FileSystemStorage.fs.sink(pos).buffer().use { p ->
                p.writeLong(0L)
                val bytes1 = dataFileEntryFormat.encodeToByteArray(
                    DataFileEntry(
                        CarStreamType.id,
                        uuid4().toString(),
                        1,
                        CarStreamType.binaryEventSerializer.serialize(carProduced1),
                        DefaultEventMetadataSerializer.serialize(mapOf()),
                    ),
                )
                d.writeInt(bytes1.size)
                d.write(bytes1)
                d.writeInt(bytes1.size)
                d.writeLong(1L)

                p.writeLong(4L + bytes1.size + 4L + 8L)
                val bytes2 = dataFileEntryFormat.encodeToByteArray(
                    DataFileEntry(
                        CarStreamType.id,
                        uuid4().toString(),
                        2,
                        CarStreamType.binaryEventSerializer.serialize(carProduced2),
                        DefaultEventMetadataSerializer.serialize(mapOf()),
                    ),
                )
                d.writeInt(bytes2.size)
                d.write(bytes2)
                d.writeInt(bytes2.size)
                d.writeLong(1L) // should be 2
            }
        }

        // when, then
        val eventStore = factory.newEventStore(storage)
        assertFailsWith<CorruptedDataException> {
            eventStore.loadEventBatch<Any, Any>(0L, 2)
        }
    }
}
