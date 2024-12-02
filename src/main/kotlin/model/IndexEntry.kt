package model

import kotlinx.serialization.Serializable

/**
 * Represents a file that has been added to the staging area using the command `fuata add <filename>`
 * @param path relative path of the file from the parent directory
 * @param hash SHA-1 value of the file content
 */
@Serializable
data class IndexEntry(
    val path: String,
    val hash: String
)