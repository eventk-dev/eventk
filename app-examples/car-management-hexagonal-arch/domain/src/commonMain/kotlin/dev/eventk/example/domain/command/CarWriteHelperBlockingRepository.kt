package dev.eventk.example.domain.command

import com.benasher44.uuid.Uuid

interface CarWriteHelperBlockingRepository {
    fun getByUuid(id: Uuid): CarWriteHelper?
    fun getOrCreate(id: Uuid, vin: String): CarWriteHelper
}
