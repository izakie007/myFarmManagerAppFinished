package com.palmfarm.manager.utils

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.text.Charsets.UTF_8

/**
 * Utility for secure password hashing using SHA-256
 */
object PasswordHasher {

    private const val HASH_ALGORITHM = "SHA-256"
    private const val SALT_LENGTH = 32

    /**
     * Hash a password with a random salt
     * Returns: "salt:hash" format
     */
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hash = hash(password, salt)
        return "$salt:$hash"
    }

    /**
     * Verify a password against a stored hash
     * @param password The plain text password to verify
     * @param storedHash The stored hash in "salt:hash" format
     * @return true if password matches, false otherwise
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false

            val salt = parts[0]
            val hash = parts[1]

            val computedHash = hash(password, salt)
            hash == computedHash
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate a random salt
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return bytesToHex(salt)
    }

    /**
     * Hash password with salt using SHA-256
     */
    private fun hash(password: String, salt: String): String {
        val saltedPassword = "$salt$password"
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(saltedPassword.toByteArray(UTF_8))
        return bytesToHex(hashBytes)
    }

    /**
     * Convert byte array to hexadecimal string
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Simple hash without salt (for backwards compatibility or simple cases)
     * Not recommended for production use
     */
    fun simpleHash(password: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(password.toByteArray(UTF_8))
        return bytesToHex(hashBytes)
    }
}
