package model

import kotlinx.serialization.Serializable

/**
 * Represents a directory or the root tree created when creating a commit from staged files
 * A node in the Directed Acyclic Graph (DAG) structure making up the version control system
 * @property type The node type in the larger Directed Acyclic Graph data structure that represents the tracked directory
 * @property entries The [blobs][Blob] or [trees][Tree] this tree references
 */
@Serializable
data class Tree(
    val type: String = "tree",
    val entries: Map<String, String> = mapOf()
)
