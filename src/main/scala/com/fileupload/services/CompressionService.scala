package com.fileupload.services

import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipCompressorOutputStream}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import scala.util.Try

class CompressionService {
  def compress(data: Array[Byte]): Try[Array[Byte]] = Try {
    val outputStream = new ByteArrayOutputStream()
    val gzipOutputStream = new GzipCompressorOutputStream(outputStream)
    
    try {
      gzipOutputStream.write(data)
      gzipOutputStream.close()
      outputStream.toByteArray
    } finally {
      gzipOutputStream.close()
      outputStream.close()
    }
  }

  def decompress(compressedData: Array[Byte]): Try[Array[Byte]] = Try {
    val inputStream = new ByteArrayInputStream(compressedData)
    val gzipInputStream = new GzipCompressorInputStream(inputStream)
    val outputStream = new ByteArrayOutputStream()
    
    try {
      val buffer = new Array[Byte](1024)
      var n = gzipInputStream.read(buffer)
      while (n != -1) {
        outputStream.write(buffer, 0, n)
        n = gzipInputStream.read(buffer)
      }
      outputStream.toByteArray
    } finally {
      gzipInputStream.close()
      inputStream.close()
      outputStream.close()
    }
  }
} 