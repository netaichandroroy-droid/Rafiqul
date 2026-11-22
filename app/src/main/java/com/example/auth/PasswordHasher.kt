package com.example.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM_PREFIX = "pbkdf2_sha256"
    private const val JCE_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_LENGTH_BITS = 256
    private const val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private val secureRandom = SecureRandom()

    fun hashPassword(rawPassword: String, minLength: Int = 8): String {
        require(rawPassword.length in minLength..128) {
            "পাসওয়ার্ড $minLength থেকে ১২৮ অক্ষরের মধ্যে হতে হবে।"
        }
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val derived = deriveKey(
            password = rawPassword.toCharArray(),
            salt = salt,
            iterations = DEFAULT_ITERATIONS,
            keyLengthBits = KEY_LENGTH_BITS
        )
        return "$ALGORITHM_PREFIX$$DEFAULT_ITERATIONS$${encodeBase64(salt)}$${encodeBase64(derived)}"
    }

    fun verifyPassword(rawPassword: String, storedHash: String): Boolean {
        if (rawPassword.isEmpty() || storedHash.isBlank()) return false
        val parts = storedHash.split('$')
        if (parts.size != 4) return false
        if (parts[0] != ALGORITHM_PREFIX) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations < 10_000 || iterations > 1_000_000) return false
        val salt = decodeBase64(parts[2]) ?: return false
        val expectedHash = decodeBase64(parts[3]) ?: return false
        if (salt.isEmpty() || expectedHash.isEmpty()) return false

        val actualHash = deriveKey(
            password = rawPassword.toCharArray(),
            salt = salt,
            iterations = iterations,
            keyLengthBits = expectedHash.size * 8
        )
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int
    ): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLengthBits)
        return try {
            val factory = SecretKeyFactory.getInstance(JCE_ALGORITHM)
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        val sb = StringBuilder(((bytes.size + 2) / 3) * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val hasB1 = i + 1 < bytes.size
            val hasB2 = i + 2 < bytes.size
            val b1 = if (hasB1) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (hasB2) bytes[i + 2].toInt() and 0xFF else 0
            val triple = (b0 shl 16) or (b1 shl 8) or b2

            sb.append(BASE64_CHARS[(triple ushr 18) and 0x3F])
            sb.append(BASE64_CHARS[(triple ushr 12) and 0x3F])
            sb.append(if (hasB1) BASE64_CHARS[(triple ushr 6) and 0x3F] else '=')
            sb.append(if (hasB2) BASE64_CHARS[triple and 0x3F] else '=')
            i += 3
        }
        return sb.toString()
    }

    private fun decodeBase64(input: String): ByteArray? {
        if (input.isEmpty() || input.length % 4 != 0) return null
        val out = ArrayList<Byte>((input.length / 4) * 3)
        var i = 0
        while (i < input.length) {
            val c0 = decodeChar(input[i]) ?: return null
            val c1 = decodeChar(input[i + 1]) ?: return null
            val pad2 = input[i + 2] == '='
            val pad3 = input[i + 3] == '='
            if (pad2 && !pad3) return null
            val c2 = if (pad2) 0 else decodeChar(input[i + 2]) ?: return null
            val c3 = if (pad3) 0 else decodeChar(input[i + 3]) ?: return null

            val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3
            out.add(((triple ushr 16) and 0xFF).toByte())
            if (!pad2) out.add(((triple ushr 8) and 0xFF).toByte())
            if (!pad3) out.add((triple and 0xFF).toByte())
            i += 4
        }
        return out.toByteArray()
    }

    private fun decodeChar(ch: Char): Int? = when (ch) {
        in 'A'..'Z' -> ch - 'A'
        in 'a'..'z' -> ch - 'a' + 26
        in '0'..'9' -> ch - '0' + 52
        '+' -> 62
        '/' -> 63
        else -> null
    }
}
