package dev.eskt.example.app.bookmark

import dev.eskt.arch.hex.port.Bookmark
import org.springframework.data.repository.CrudRepository
import kotlin.jvm.optionals.getOrNull

interface BookmarkRepository : CrudRepository<BookmarkEntity, String>, Bookmark {
    override fun get(id: String): Long {
        return findById(id).getOrNull()?.value ?: 0L
    }

    override fun set(id: String, value: Long) {
        save(BookmarkEntity(id, value))
    }
}
