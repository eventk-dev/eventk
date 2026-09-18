package dev.eventk.store.impl.pg

/** Unchecked wrapper for a failure raised by general Postgres storage operations. */
public class PostgresqlStorageException(cause: Throwable) : RuntimeException(cause.message, cause)
