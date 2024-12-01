package util

import java.security.MessageDigest

object Hashing {

    /**
     * Returns a 40-digit hexadecimal hashed value as a [String]
     * This method uses the SHA-1 algorithm to generate the hash
     * This is useful in adhering to the content-addressable file system
     * @param content The string value to be hashed
     * @return a 40-digit hexadecimal hashed value as a String
     */
    fun generateHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}