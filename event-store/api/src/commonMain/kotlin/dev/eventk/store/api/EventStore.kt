package dev.eventk.store.api

public interface EventStore {
    public val registeredTypes: Set<StreamType<*, *>>
}
