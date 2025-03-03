package com.fileupload.models

case class FileChunk(
    fileId: String,
    chunkNumber: Int,
    data: Array[Byte],
    checksum: String
)

case class ChunkMetadata(
    fileId: String,
    chunkNumber: Int,
    size: Long,
    checksum: String
) 