package dev.eventk.arch.hex.adapter.common

public interface Logger {
    public fun info(message: () -> String)
    public fun debug(message: () -> String)
    public fun error(t: Throwable, message: () -> String)
}
