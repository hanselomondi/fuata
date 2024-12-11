package util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Commit
import model.Tree
import java.lang.StringBuilder
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
        refsDirectory: String,
        indexFile: String
    ): Result<String> {
        return try {
            lateinit var newCommit: Commit
            val parentCommit = getParentCommit(parentCommitHash, objectsDirectory)
            println("parentCommit: ${parentCommit ?: "no parentCommit"}")
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
            // Clear the index file
            val index = Paths.get(indexFile)
            Files.writeString(index, Json.encodeToString(emptyList<String>()))
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


    /**
     * Displays all the commit history from the existing commit, starting at the commit currently pointed to by HEAD
     * @param head File path to the HEAD file in .fuata repository proper
     */
    fun logCommitHistory(
        head: String,
        objectsDirectory: String,
    ) {
        // Read ref being pointed to by HEAD
        val currentBranch = Files.readString(Paths.get(head)).also { println("currentBranch: $it") }
        // Read the hash in refs/heads/<branch_name>
        var headCommitHash = Files.readString(Paths.get(".fuata/$currentBranch"))
        if (headCommitHash.isEmpty()) {
            println("No commits made yet")
            return
        }
        // Get the first commit by reading extracting the contents of refs/heads/<branch_name>
        var headCommit = getParentCommit(headCommitHash, objectsDirectory)
        // Iteratively loop through all commits that point to each other in this linked list that's a subset of the DAG
        // Stop when you reach the first commit (last node in the list)
        while (headCommit != null) {
            // Print the contents of the commit
            println("\n------------------------------------")
            println("\ncommit $headCommitHash")
            println("Author: ${headCommit.author}")
            println("Date:   ${displayCommitTimestamp(headCommit.timestamp)}")
            println("\n    ${headCommit.message}\n")

            // Move to the next commit in the graph
            headCommitHash = headCommit.parent
            headCommit = getParentCommit(headCommitHash, objectsDirectory)
        }
    }

    /**
     * Displays the diff between two commits[Commit]
     * Retrieves the root tree[Tree] of one commit and compares its entries to those of the other commit
     * @param commit1Hash Commit hash of the first commit that is the reference point (baseline)
     * @param commit2Hash Commit hash of the second commit that is the revision
     * @throws Exception
     */
    fun diff(
        commit1Hash: String,
        commit2Hash: String,
        objectsDirectory: String
    ): Result<String> {
        return try {
            // Retrieve commit from objects directory and their root trees
            val commit1 = getParentCommit(commit1Hash, objectsDirectory)
                ?: return Result.failure(Exception("commit hash `$commit1Hash` not found"))
            val rootTree1 = TreeUtil.getTree(commit1.tree, objectsDirectory)
                .getOrElse { throw it }
            val commit2 = getParentCommit(commit2Hash, objectsDirectory)
                ?: return Result.failure(Exception("commit hash `$commit2Hash` not found"))
            val rootTree2 = TreeUtil.getTree(commit2.tree, objectsDirectory)
                .getOrElse { throw it }
            // Compare root trees of the commits
            val diffResult = StringBuilder()
            val entries1 = rootTree1.entries
            val entries2 = rootTree2.entries
            // Record added files
            val addedFiles = entries2.keys - entries1.keys  // Get the set complement
            if (addedFiles.isNotEmpty()) {
                diffResult.append("\nAdded files:\n")
                addedFiles.forEach { diffResult.append("  $it\n") }
            }
            // Record deleted files if any
            val deletedFiles = entries1.keys - entries2.keys
            if (deletedFiles.isNotEmpty()) {
                diffResult.append("\nDeleted files:\n")
                deletedFiles.forEach { diffResult.append("  $it\n") }
            }
            // Compare common files for changes
            val commonFiles = entries1.keys.intersect(entries2.keys)
            val modifiedFiles = commonFiles.filter { filePath -> entries1[filePath] != entries2[filePath] }
            if (modifiedFiles.isNotEmpty()) {
                diffResult.append("\nModified files:\n")
                for (file in modifiedFiles) {
                    diffResult.append("  $file\n")
                    val content1 = Compression.decompressData(
                        Files.readAllBytes(Paths.get(objectsDirectory, file))
                    )
                    val content2 = Compression.decompressData(
                        Files.readAllBytes(Paths.get(objectsDirectory, file))
                    )
                    diffResult.append(compareFileContents(content1, content2))
                }
            }

            Result.success(diffResult.toString())
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "CommitUtil.diff() : unknown error"))
        }
    }

    /**
     * Compares the content of two files line-by-line
     * @param content1 Content of the first file
     * @param content2 Content of the second file
     * @return A String containing all the differences
     */
    private fun compareFileContents(content1: String, content2: String): String {
        val lines1 = content1.lines()
        val lines2 = content2.lines()

        val diffResult = StringBuilder()

        // Simple line-by-line comparison
        val maxLines = maxOf(lines1.size, lines2.size)
        for (i in 0 until maxLines) {
            val line1 = lines1.getOrNull(i)
            val line2 = lines2.getOrNull(i)

            if (line1 != line2) {
                diffResult.append("Line ${i + 1}:\n")
                diffResult.append("  -- $line1\n")
                diffResult.append("  ++ $line2\n")
            }
        }

        return diffResult.toString()
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