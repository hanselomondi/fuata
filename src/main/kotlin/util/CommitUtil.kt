package util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Commit
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
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
     * @param refsDirectory File path to .fuata/refs/ directory
     * @return Hash of the newly created commit object
     * @throws Exception If this operation fails
     */
    fun createCommit(
        message: String,
        parentCommitHash: String?,
        stagedFiles: Map<String, String>,  // Map<filePath, fileHash>
        objectsDirectory: String,
        refsDirectory: String
    ): Result<String> {
        return try {
            lateinit var newCommit: Commit
            val parentCommit = getParentCommit(parentCommitHash, objectsDirectory)
            println("parentCommit: $parentCommit ?: no parentCommit")
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
            // Update the hash in the references folder
            val refsFile = Paths.get(refsDirectory)
            Files.writeString(refsFile, newCommitHash)
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
        println("getParentCommit invoked with commitHash: $commitHash")
        if (commitHash.isNullOrBlank()) {
            println("No parent commit hash found. This might be the first commit.")
            return null
        }

        val parentCommitPath = Paths.get(objectDirectory, commitHash)
        if (!Files.exists(parentCommitPath)) {
            println("Parent commit file not found: $parentCommitPath")
            return null
        }

        return try {
            val parentCommitObject = Files.readAllBytes(parentCommitPath)
            val parentCommitJson = Compression.decompressData(parentCommitObject)
            println("Decompressed parent commit JSON: $parentCommitJson")
            Json.decodeFromString<Commit>(parentCommitJson)
        } catch (e: Exception) {
            println("Error reading or parsing parent commit: ${e.message}")
            throw Exception("Failed to get parent commit")
        }
    }


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

    /**
     * Returns the UtcTimestamp as a formatted String
     */
    private fun displayCommitTimestamp(utcTimestamp: String): String {
        // Parse the UTC ISO 8601 string into an Instant
        val instant = Instant.parse(utcTimestamp)

        // Convert the Instant to the local timezone
        val localTime = instant.atZone(ZoneId.systemDefault())

        // Format the local time as a human-readable string
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
        return localTime.format(formatter)
    }
}