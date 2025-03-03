package com.fileupload.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, Sink}
import akka.util.ByteString
import com.fileupload.services.{FileStorageService, EncryptionService, CompressionService}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

class FileDownloadRoute(
    storageService: FileStorageService,
    encryptionService: EncryptionService,
    compressionService: CompressionService
)(implicit ec: ExecutionContext, mat: Materializer) {

  private def parseRange(rangeHeader: String): Option[(Long, Long)] = {
    val pattern = "bytes=(\\d+)-(\\d*)".r
    rangeHeader match {
      case pattern(start, end) if end.nonEmpty => Some((start.toLong, end.toLong))
      case pattern(start, _) => Some((start.toLong, Long.MaxValue))
      case _ => None
    }
  }

  val route: Route = {
    path("api" / "v1" / "files" / "download" / Segment) { fileId =>
      get {
        optionalHeaderValueByName("Range") { rangeHeader =>
          onComplete(storageService.getFileMetadata(fileId)) {
            case Success(Some(metadata)) =>
              val range = rangeHeader.flatMap(parseRange)
              
              val chunkSource = Source(0 until metadata.chunks)
                .mapAsync(1) { chunkNumber =>
                  storageService.getChunk(fileId, chunkNumber).map {
                    case Some(chunk) =>
                      val (encrypted, iv) = chunk.data.splitAt(chunk.data.length - 16)
                      for {
                        decrypted <- encryptionService.decrypt(encrypted, iv)
                        decompressed <- compressionService.decompress(decrypted)
                      } yield ByteString(decompressed)
                    case None =>
                      throw new Exception(s"Chunk $chunkNumber not found")
                  }.flatMap(Future.fromTry)
                }

              val responseEntity = range match {
                case Some((start, end)) =>
                  val contentLength = if (end == Long.MaxValue) metadata.size - start else end - start + 1
                  HttpEntity.Default(
                    ContentTypes.`application/octet-stream`,
                    contentLength,
                    chunkSource.drop(start.toInt / metadata.size.toInt * metadata.chunks)
                  )
                case None =>
                  HttpEntity.Default(
                    ContentTypes.`application/octet-stream`,
                    metadata.size,
                    chunkSource
                  )
              }

              val headers = List(
                headers.`Content-Disposition`(
                  ContentDispositionTypes.attachment,
                  Map("filename" -> metadata.filename)
                )
              ) ++ range.map { case (start, end) =>
                headers.`Content-Range`(ContentRange(start, end, metadata.size))
              }

              complete(
                HttpResponse(
                  status = range.map(_ => StatusCodes.PartialContent).getOrElse(StatusCodes.OK),
                  headers = headers,
                  entity = responseEntity
                )
              )

            case Success(None) =>
              complete(StatusCodes.NotFound -> "File not found")

            case Failure(ex) =>
              complete(StatusCodes.InternalServerError -> ex.getMessage)
          }
        }
      }
    }
  }
} 