package model

import kotlinx.serialization.Serializable

/**
 * Represents a commit created from staged files
 * @property type The node type in the larger Directed Acyclic Graph data structure that represents the tracked directory
 * @property tree The root [tree][Tree], created when committing staged files, being referenced by this commit
 * @property parent The previous commit that HEAD was pointing to
 * @property author The name and email of the creator of this commit
 * @property timestamp The UNIX timestamp when this commit was created
 * @property message The commit message
 */
@Serializable
data class Commit(
    val type: String = "commit",
    val tree: String,
    val parent: String?,
    val author: String = "John Doe <john.doe@gmail.com>",
    val timestamp: String,
    val message: String
)
