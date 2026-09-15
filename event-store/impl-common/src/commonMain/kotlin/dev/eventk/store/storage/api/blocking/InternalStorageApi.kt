package dev.eventk.store.storage.api.blocking

/**
 * Marks [Storage] as an internal SPI implemented by eventk's own built-in backends (in-memory, filesystem,
 * PostgreSQL). It is not a stable public API: its shape may change incompatibly between any two releases,
 * without deprecation. Implementing it outside of eventk is possible but unsupported.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Storage is eventk's internal storage SPI, implemented by its own built-in backends. " +
        "It is not a stable public API and may change incompatibly between releases without notice.",
)
@Retention(AnnotationRetention.BINARY)
public annotation class InternalStorageApi
