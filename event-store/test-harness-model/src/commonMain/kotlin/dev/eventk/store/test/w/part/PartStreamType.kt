package dev.eventk.store.test.w.part

import dev.eventk.store.api.StreamType

public object PartStreamType : StreamType<PartEvent, String> {
    override val id: String = "Part"
}
