package com.palmfarm.manager.utils

import java.security.MessageDigest

/**
 * Security utilities for password hashing and other security operations
 */
object SecurityUtils {

    /**
     * Hash password using SHA-256
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify password against hash
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}
