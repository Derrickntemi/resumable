package com.fileupload.services

import com.fileupload.models.{FileMetadata, FileChunk}
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID

class FileStorageService(mongoClient: MongoClient)(implicit ec: ExecutionContext) {
  private val database = mongoClient.getDatabase("fileupload")
  private val filesCollection = database.getCollection("files")
  private val chunksCollection = database.getCollection("chunks")

  def createFile(filename: String, contentType: String, size: Long, uploadedBy: String, totalChunks: Int): Future[String] = {
    val fileId = UUID.randomUUID().toString
    val metadata = FileMetadata(
      id = fileId,
      filename = filename,
      contentType = contentType,
      size = size,
      uploadedBy = uploadedBy,
      uploadedAt = Instant.now(),
      chunks = totalChunks,
      completed = false,
      checksum = ""
    )

    val doc = Document(
      "id" -> metadata.id,
      "filename" -> metadata.filename,
      "contentType" -> metadata.contentType,
      "size" -> metadata.size,
      "uploadedBy" -> metadata.uploadedBy,
      "uploadedAt" -> metadata.uploadedAt.toString,
      "chunks" -> metadata.chunks,
      "completed" -> metadata.completed,
      "checksum" -> metadata.checksum
    )

    filesCollection.insertOne(doc).toFuture().map(_ => fileId)
  }

  def saveChunk(chunk: FileChunk): Future[Unit] = {
    val doc = Document(
      "fileId" -> chunk.fileId,
      "chunkNumber" -> chunk.chunkNumber,
      "data" -> chunk.data,
      "checksum" -> chunk.checksum
    )

    chunksCollection.insertOne(doc).toFuture().map(_ => ())
  }

  def getChunk(fileId: String, chunkNumber: Int): Future[Option[FileChunk]] = {
    chunksCollection
      .find(and(equal("fileId", fileId), equal("chunkNumber", chunkNumber)))
      .first()
      .toFutureOption()
      .map(_.map(doc => FileChunk(
        fileId = doc.getString("fileId"),
        chunkNumber = doc.getInteger("chunkNumber"),
        data = doc.get("data").asInstanceOf[Array[Byte]],
        checksum = doc.getString("checksum")
      )))
  }

  def getFileMetadata(fileId: String): Future[Option[FileMetadata]] = {
    filesCollection
      .find(equal("id", fileId))
      .first()
      .toFutureOption()
      .map(_.map(doc => FileMetadata(
        id = doc.getString("id"),
        filename = doc.getString("filename"),
        contentType = doc.getString("contentType"),
        size = doc.getLong("size"),
        uploadedBy = doc.getString("uploadedBy"),
        uploadedAt = Instant.parse(doc.getString("uploadedAt")),
        chunks = doc.getInteger("chunks"),
        completed = doc.getBoolean("completed"),
        checksum = doc.getString("checksum")
      )))
  }

  def markFileAsComplete(fileId: String, checksum: String): Future[Unit] = {
    filesCollection
      .updateOne(
        equal("id", fileId),
        combine(
          set("completed", true),
          set("checksum", checksum)
        )
      )
      .toFuture()
      .map(_ => ())
  }
} 