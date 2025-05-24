package dev.eskt.example.app.command

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table(
    name = "car_write_helper",
//    uniqueConstraints = [
//        UniqueConstraint(name = "unique_vin", columnNames = ["vin"]),
//    ],
)
data class CarWriteHelperEntity(
//    @Id
    val id: UUID,
    val vin: String,
)
