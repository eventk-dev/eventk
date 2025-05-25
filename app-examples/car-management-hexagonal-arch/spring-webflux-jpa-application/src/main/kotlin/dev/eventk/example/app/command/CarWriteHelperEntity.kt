package dev.eventk.example.app.command

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.*

@Entity
@Table(
    name = "car_write_helper",
    uniqueConstraints = [
        UniqueConstraint(name = "unique_vin", columnNames = ["vin"]),
    ],
)
data class CarWriteHelperEntity(
    @Id
    val id: UUID,
    val vin: String,
)
