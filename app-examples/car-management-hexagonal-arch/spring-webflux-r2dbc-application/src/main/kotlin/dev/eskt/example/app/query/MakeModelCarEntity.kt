package dev.eskt.example.app.query

import com.benasher44.uuid.Uuid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(name = "car_count_rm__car")
data class MakeModelCarEntity(
    @Id
    val id: Uuid,
    val make: String,
    val model: String,
)
