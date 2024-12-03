package util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Commit
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object CommitUtil {

    /**
     * Creates a commit object from the staged files
     * @param message Commit message
     * @param parentCommitHash Commit hash the HEAD is currently referencing at the time of creating a new commit
     * @param stagedFiles A list of files that are in .fuata/index and their metadata
     * @param objectsDirectory File path to .fuata/objects/ directory
     * @return Hash of the newly created commit object
     * @throws Exception If this operation fails
     */
    fun createCommit(
        message: String,
        parentCommitHash: String?,
        stagedFiles: Map<String, String>,  // Map<filePath, fileHash>
        objectsDirectory: String
    ): Result<String> {
        return try {
            lateinit var newCommit: Commit
            val parentCommit = getParentCommit(parentCommitHash, objectsDirectory)
            // Get parentTreeHash
            val parentTreeHash = parentCommit?.tree
            // Derive the parentTree (tree referenced by the parent commit)
            val parentTree = parentTreeHash?.let { treeHash ->
                TreeUtil.getTree(treeHash, objectsDirectory)
                    .fold(
                        onSuccess = { it },
                        onFailure = { e -> throw e }
                    )
            }
            // Create the root tree for the new commit and return its hash
            val rootTreeHash = TreeUtil
                .createTree(
                    stagedFiles = stagedFiles,
                    parentTree = parentTree,
                    objectsDirPath = objectsDirectory
                )
                .fold(
                    onSuccess = { it },
                    onFailure = { e -> throw e }
                )
            // Create the new commit
            newCommit = Commit(
                tree = rootTreeHash,
                parent = parentCommitHash,
                timestamp = getCurrentUtcIsoTimestamp(),
                message = message
            )
            // Write the new commit into an object file
            val newCommitJson = Json.encodeToString(newCommit)
            println("CommitUtil.createCommit() : newCommitJson: $newCommitJson")
            val newCommitHash = Hashing.generateHash(newCommitJson)
            val compressedJson = Compression.compressData(newCommitJson)
            Files.write(Paths.get(objectsDirectory, newCommitHash), compressedJson)
            // Return the new commit hash
            Result.success(newCommitHash)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "CommitUtil.createCommit() : unknown error"))
        }
    }

    /**
     * Reads a file in the objects directory and deserializes it to return a [commit][Commit] object
     * This commit is the commit currently being pointed to by HEAD at the time of creating a new commit
     * @param commitHash File path of commit in .fuata/objects
     * @param objectDirectory File path of .fuata/objects
     * @return Parent Commit object
     */
    private fun getParentCommit(commitHash: String?, objectDirectory: String): Commit? {
        return commitHash?.let { hash ->
            val parentCommitObject = Files.readAllBytes(Paths.get(objectDirectory, hash))
            val parentCommitJson = Compression.decompressData(parentCommitObject)
            println("CommitUtil.getParentCommit() : parentCommit Json: $parentCommitJson")
            Json.decodeFromString(parentCommitJson)
        }
    }

    /**
     * Returns the local time as an ISO string
     */
    private fun getCurrentUtcIsoTimestamp(): String {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val utcNow = now.withZoneSameInstant(ZoneId.of("UTC"))
        return utcNow.format(DateTimeFormatter.ISO_INSTANT) // Format as ISO 8601 string
    }
}