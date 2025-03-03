package com.fileupload

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.fileupload.routes.{FileUploadRoute, FileDownloadRoute}
import com.fileupload.services.{FileStorageService, EncryptionService, CompressionService}
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.{MongoClient, MongoClientSettings}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import scala.io.StdIn

object Main extends App {
  implicit val system = ActorSystem(Behaviors.empty, "FileUploadSystem")
  implicit val executionContext = system.executionContext

  val config = ConfigFactory.load()
  
  // Initialize MongoDB client
  val mongoClient = MongoClient(config.getString("mongodb.uri"))
  
  // Initialize services
  val storageService = new FileStorageService(mongoClient)
  val encryptionService = new EncryptionService(
    config.getString("file-upload.security.encryption-key")
  )
  val compressionService = new CompressionService()

  // Initialize routes
  val uploadRoute = new FileUploadRoute(
    storageService,
    encryptionService,
    compressionService
  )
  val downloadRoute = new FileDownloadRoute(
    storageService,
    encryptionService,
    compressionService
  )

  val routes = uploadRoute.route ~ downloadRoute.route

  // Start the server
  val bindingFuture = Http().newServerAt("localhost", 8080).bind(routes)

  println(s"Server online at http://localhost:8080/")
  println(s"Press RETURN to stop...")
  
  StdIn.readLine()
  
  bindingFuture
    .flatMap(_.unbind())
    .onComplete { _ =>
      mongoClient.close()
      system.terminate()
    }
} 