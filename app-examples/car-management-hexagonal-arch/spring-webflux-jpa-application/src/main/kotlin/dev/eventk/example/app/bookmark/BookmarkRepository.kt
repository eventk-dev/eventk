package dev.eventk.example.app.bookmark

import dev.eventk.arch.hex.port.Bookmark
import org.springframework.data.jpa.repository.JpaRepository
import kotlin.jvm.optionals.getOrNull

interface BookmarkRepository : JpaRepository<BookmarkEntity, String>, Bookmark {
    override fun get(id: String): Long {
        return findById(id).getOrNull()?.value ?: 0L
    }

    override fun set(id: String, value: Long) {
        save(BookmarkEntity(id, value))
    }
}
