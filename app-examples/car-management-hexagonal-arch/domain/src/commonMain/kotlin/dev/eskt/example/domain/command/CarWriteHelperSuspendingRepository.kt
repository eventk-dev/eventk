package dev.eskt.example.domain.command

import com.benasher44.uuid.Uuid

interface CarWriteHelperSuspendingRepository {
    suspend fun getByUuid(id: Uuid): CarWriteHelper?
    suspend fun getOrCreate(id: Uuid, vin: String): CarWriteHelper
}
