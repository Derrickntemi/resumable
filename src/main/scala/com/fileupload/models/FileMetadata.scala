package com.fileupload.models

import java.time.Instant
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class FileMetadata(
    id: String,
    filename: String,
    contentType: String,
    size: Long,
    uploadedBy: String,
    uploadedAt: Instant,
    chunks: Int,
    completed: Boolean,
    checksum: String
)

object FileMetadata {
  implicit val instantFormat: RootJsonFormat[Instant] = new RootJsonFormat[Instant] {
    def write(instant: Instant) = spray.json.JsString(instant.toString)
    def read(value: spray.json.JsValue) = Instant.parse(value.convertTo[String])
  }

  implicit val fileMetadataFormat: RootJsonFormat[FileMetadata] = jsonFormat9(FileMetadata.apply)
} 