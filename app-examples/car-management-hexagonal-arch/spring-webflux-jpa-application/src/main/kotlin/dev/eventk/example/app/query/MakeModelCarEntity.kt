package dev.eventk.example.app.query

import com.benasher44.uuid.Uuid
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "car_count_rm__car")
data class MakeModelCarEntity(
    @Id
    val id: Uuid,
    val make: String,
    val model: String,
)
