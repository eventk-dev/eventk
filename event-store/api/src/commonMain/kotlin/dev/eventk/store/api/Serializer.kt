package dev.eventk.store.api

public interface Serializer<O, S> {
    public fun serialize(obj: O): S
    public fun deserialize(payload: S): O
}
