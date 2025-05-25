package dev.eventk.example.app.bookmark

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "bookmark")
@Entity
data class BookmarkEntity(
    @Id
    val id: String,
    val value: Long,
)
