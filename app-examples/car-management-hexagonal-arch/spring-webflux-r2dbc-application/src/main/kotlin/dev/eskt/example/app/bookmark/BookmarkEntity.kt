package dev.eskt.example.app.bookmark

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table


@Table(name = "bookmark")
data class BookmarkEntity(
    @Id
    val id: String,
    val value: Long,
)
