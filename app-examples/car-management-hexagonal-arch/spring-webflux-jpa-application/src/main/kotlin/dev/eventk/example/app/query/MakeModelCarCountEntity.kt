package dev.eventk.example.app.query

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "car_count_rm__car_count")
data class MakeModelCarCountEntity(
    @EmbeddedId
    val id: Id,
    val count: Int,
) {
    data class Id(
        val make: String,
        val model: String,
    ) : Serializable {
        constructor() : this("", "")
    }
}
