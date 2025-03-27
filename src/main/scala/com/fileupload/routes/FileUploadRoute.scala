package com.fileupload.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{StatusCodes, HttpResponse, ContentTypes}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, Sink}
import akka.util.ByteString
import com.fileupload.services.{FileStorageService, EncryptionService, CompressionService}
import com.fileupload.models.{FileChunk, FileMetadata}
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.concurrent.{ExecutionContext, Future}
import java.security.MessageDigest

class FileUploadRoute(
    storageService: FileStorageService,
    encryptionService: EncryptionService,
    compressionService: CompressionService
)(implicit ec: ExecutionContext, mat: Materializer) {

  private def calculateChecksum(data: Array[Byte]): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.digest(data).map("%02x".format(_)).mkString
  }

  val route: Route = {
    path("api" / "v1" / "files" / "upload") {
      post {
        extractRequestEntity { entity =>
          headerValueByName("X-Upload-ID") { uploadId =>
            headerValueByName("X-Chunk-Number") { chunkNumber =>
              headerValueByName("X-Total-Chunks") { totalChunks =>
                val chunkNumberInt = chunkNumber.toInt
                val totalChunksInt = totalChunks.toInt

                val futureResponse = entity.dataBytes
                  .runFold(ByteString.empty)(_ ++ _)
                  .flatMap { data =>
                    val rawData = data.toArray
                    val checksum = calculateChecksum(rawData)

                    for {
                      compressed <- compressionService.compress(rawData)
                      (encrypted, iv) <- Future.fromTry(encryptionService.encrypt(compressed))
                      chunk = FileChunk(uploadId, chunkNumberInt, encrypted ++ iv, checksum)
                      _ <- storageService.saveChunk(chunk)
                      _ <- if (chunkNumberInt == totalChunksInt - 1) {
                        storageService.markFileAsComplete(uploadId, checksum)
                      } else Future.successful(())
                    } yield {
                      JsObject(
                        "uploadId" -> JsString(uploadId),
                        "chunkReceived" -> JsNumber(chunkNumberInt),
                        "status" -> JsString(if (chunkNumberInt == totalChunksInt - 1) "completed" else "in_progress"),
                        "fileId" -> JsString(uploadId)
                      ).toString
                    }
                  }

                onComplete(futureResponse) {
                  case scala.util.Success(response) =>
                    complete(HttpResponse(
                      status = StatusCodes.OK,
                      entity = response
                    ))
                  case scala.util.Failure(ex) =>
                    complete(HttpResponse(
                      status = StatusCodes.InternalServerError,
                      entity = ex.getMessage
                    ))
                }
              }
            }
          }
        }
      }
    }
  }
} 