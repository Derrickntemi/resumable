package com.fileupload.services

import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import java.security.SecureRandom
import java.util.Base64
import scala.util.{Try, Success, Failure}

class EncryptionService(key: String) {
  private val algorithm = "AES/CBC/PKCS5Padding"
  private val keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES")
  private val random = new SecureRandom()

  def encrypt(data: Array[Byte]): Try[(Array[Byte], Array[Byte])] = Try {
    val cipher = Cipher.getInstance(algorithm)
    val iv = new Array[Byte](16)
    random.nextBytes(iv)
    val ivSpec = new IvParameterSpec(iv)
    
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
    val encrypted = cipher.doFinal(data)
    (encrypted, iv)
  }

  def decrypt(encryptedData: Array[Byte], iv: Array[Byte]): Try[Array[Byte]] = Try {
    val cipher = Cipher.getInstance(algorithm)
    val ivSpec = new IvParameterSpec(iv)
    
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    cipher.doFinal(encryptedData)
  }
} 