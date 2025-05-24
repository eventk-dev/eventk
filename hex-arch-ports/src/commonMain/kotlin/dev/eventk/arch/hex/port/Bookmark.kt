package dev.eventk.arch.hex.port

public interface Bookmark {
    public fun get(id: String): Long
    public fun set(id: String, value: Long)
}
