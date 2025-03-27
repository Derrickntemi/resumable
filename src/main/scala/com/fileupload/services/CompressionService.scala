package com.fileupload.services

import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipCompressorOutputStream}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, OutputStream}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}

trait CompressionService {
  def compress(data: Array[Byte]): Future[Array[Byte]]

  def decompress(data: Array[Byte]): Future[Array[Byte]]
}


object CompressionService {
  def apply()(implicit executionContext: ExecutionContext): CompressionService = {
    CompressionServiceImpl()
  }
}

case class CompressionServiceImpl()(implicit executionContext: ExecutionContext) extends CompressionService {
  override def compress(data: Array[Byte]): Future[Array[Byte]] = Future {
    val outputStream = new ByteArrayOutputStream()
    val gzipOutputStream = new GzipCompressorOutputStream(outputStream)

    Using.resources(outputStream, gzipOutputStream) { (bos: ByteArrayOutputStream, compressor: GzipCompressorOutputStream) =>
      compressor.write(data)
      bos.toByteArray
    }
  }


  override def decompress(compressedData: Array[Byte]): Future[Array[Byte]] = Future {
    val inputStream = new ByteArrayInputStream(compressedData)
    val gzipInputStream = new GzipCompressorInputStream(inputStream)
    val outputStream = new ByteArrayOutputStream()

    Using.resources(inputStream, gzipInputStream, outputStream) { (is, decompressor, os) =>
      decompressor.transferTo(os)
      os.toByteArray
    }
  }
} 
