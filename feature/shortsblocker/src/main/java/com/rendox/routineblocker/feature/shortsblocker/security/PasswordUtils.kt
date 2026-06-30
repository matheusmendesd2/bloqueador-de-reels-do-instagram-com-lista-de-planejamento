package com.rendox.routineblocker.feature.shortsblocker.security

import java.security.MessageDigest

object PasswordUtils {
    private const val SALT = "RoutineBlocker2024_Salt!"

    fun hash(password: String): String {
        val salted = password + SALT
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, hash: String): Boolean {
        return hash(password) == hash
    }
}
