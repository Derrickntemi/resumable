package com.fileupload.models

case class FileChunk(
    fileId: String,
    chunkNumber: Int,
    data: Array[Byte],
    checksum: String,
    range: ByteRange
)

case class ByteRange(start: Int, end: Int)

case class ChunkMetadata(
    fileId: String,
    chunkNumber: Int,
    size: Long,
    checksum: String,
    range: ByteRange
) 