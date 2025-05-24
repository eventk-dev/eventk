package dev.eskt.example.app.query

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable

@Table(name = "car_count_rm__car_count")
data class MakeModelCarCountEntity(
    @Id
    val id: Cid,
    val count: Int,
) {
    data class Cid(
        val make: String,
        val model: String,
    ) : Serializable {
        constructor() : this("", "")
    }
}
