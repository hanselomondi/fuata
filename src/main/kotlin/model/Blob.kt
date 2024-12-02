package model

import kotlinx.serialization.Serializable


/**
 * A blob object representing a file that is being tracked by the version control system
 * @property type The node type in the larger Directed Acyclic Graph data structure that represents the tracked directory
 * @property path The relative path of the file being tracked
 * @property fileContent The actual content written in the file being tracked
 */
@Serializable
data class Blob(
    val type: String = "blob",
    val path: String,
    val fileContent: String
)