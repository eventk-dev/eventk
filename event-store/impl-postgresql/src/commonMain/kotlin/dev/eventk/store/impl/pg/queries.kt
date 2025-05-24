package dev.eventk.store.impl.pg

internal fun selectEventByPositionSql(info: TableInfo) = """
    select position, stream_type, stream_id, version, payload, metadata 
    from ${info.table}
    where position = ?;
    """.trimIndent()

internal fun selectEventSincePositionSql(info: TableInfo) = """
    select position, stream_type, stream_id, version, payload, metadata 
    from ${info.table}
    where position > ?
    order by position asc
    limit ?;
    """.trimIndent()

internal fun selectEventByTypeSincePositionSql(info: TableInfo) = """
    select position, stream_type, stream_id, version, payload, metadata 
    from ${info.table}
    where stream_type = ? and position > ?
    order by position asc
    limit ?;
    """.trimIndent()

internal fun selectMaxVersionByStreamIdSql(info: TableInfo) = """
    select max(version) 
    from ${info.table}
    where stream_id = ?;
    """.trimIndent()

internal fun selectEventByStreamIdAndVersionSql(info: TableInfo) = """
    select position, stream_type, stream_id, version, payload, metadata 
    from ${info.table}
    where stream_id = ? and version > ?
    order by version asc
    limit ?;
    """.trimIndent()

internal fun insertEventSql(info: TableInfo) = """
    insert into ${info.writeTable} (stream_type, stream_id, version, payload, metadata)
    values (?, ?, ?, ?, ?);    
    """.trimIndent()
