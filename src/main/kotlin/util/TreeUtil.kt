package util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Tree
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths

object TreeUtil {

    /**
     * Creates a [tree][Tree] when creating a commit from staged files
     * @param stagedFiles Staged files in .fuata/index
     * @param parentTree Reference to the root tree of the parent commit
     * @param objectsDirPath File path to ./fuata/objects/ directory
     * @return The newly created [tree][Tree] object
     * @throws Exception
     */
    fun createTree(
        stagedFiles: Map<String, String>,  // Map<filePath, fileHash>
        parentTree: Tree?,  // Tree object probably gotten from parsing the file in .fuata/objects/
        objectsDirPath: String
    ): Result<String> {
        return try {
            // List of staged files that are new or have changed since the last commit
            val newEntries = mutableMapOf<String, String>()

            val parentEntries = parentTree?.entries ?: mapOf()
            val directoryStructure =
                stagedFiles.keys.groupBy { filePath -> getDirectoryPath(filePath) }  // Map<dirPath, List<filePath>>
            for ((dirPath, pathsInDir) in directoryStructure) {
                if (dirPath.isNotEmpty()) {
                    // Create a new subtree for each directory and map files to it
                    val subtreeEntries = mutableMapOf<String, String>()
                    for (path in pathsInDir) {
                        val fileHash = stagedFiles[path]
                        val relativeFilePath = getRelativeFilePath(path, dirPath)
                        subtreeEntries[relativeFilePath] = fileHash ?: ""
                    }
                    val subTree = Tree(entries = subtreeEntries)
                    val serializedSubTree = Json.encodeToString(subTree)
                    println("subTree Json: $serializedSubTree")
                    val subTreeHash = Hashing.generateHash(serializedSubTree)
                    // Write subtree data to its own object file
                    Files.write(Paths.get(objectsDirPath, subTreeHash), Compression.compressData(serializedSubTree))
                    newEntries[dirPath] = subTreeHash
                }
            }

            // Blobs that are not in directories
            for ((path, hash) in stagedFiles) {
                if (!directoryStructure.keys.contains(getDirectoryPath(path))) {
                    newEntries[path] = hash
                }
            }

            // Handle deleted files
            for (path in parentEntries.keys) {
                if (!stagedFiles.containsKey(path)) {
                    newEntries.remove(path)
                }
            }

            val rootTree = Tree(entries = newEntries)
            val serializedRootTree = Json.encodeToString(rootTree)
            println("Serialised tree: $serializedRootTree")
            val compressedRootTree = Compression.compressData(serializedRootTree)
            val treeHash = Hashing.generateHash(serializedRootTree)
            println("Root tree hash: $treeHash")
            Files.write(Paths.get(objectsDirPath, treeHash), compressedRootTree)

            Result.success(treeHash)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "TreeUtil.createTree(): unknown error"))
        }
    }

    /**
     * Utility function to get the directory part of the file path (up to but not including the file name)
     */
    private fun getDirectoryPath(filePath: String): String {
        val lastSlash = filePath.lastIndexOf("\\")
        return if (lastSlash != -1) filePath.substring(0, lastSlash) else ""
    }

    /**
     * Utility function to get the relative path of a file within a directory.
     */
    private fun getRelativeFilePath(filePath: String, directoryPath: String): String {
        return filePath.substring(directoryPath.length + 1) // Strip directory part
    }

    /**
     * Reads and deserializes an object file to return a [tree][Tree] object
     * @param treeHash File name of the tree object file in the objects directory
     * @param objectsDirectory File path to .fuata/objects/ directory
     * @return Tree after deserialization from the tree object file
     * @throws IOException If the treeHash file path provided doesn't exist
     * @throws NoSuchFileException If the treeHash file path provided doesn't exist
     * @throws Exception For any other errors
     */
    fun getTree(treeHash: String, objectsDirectory: String): Result<Tree> {
        return try {
            val treeObjectFileContent = Files.readAllBytes(Paths.get(objectsDirectory, treeHash))
            val treeJson = Compression.decompressData(treeObjectFileContent)
            println("TreeUtil.getTree() : treeJson: $treeJson")
            Json.decodeFromString(treeJson)
        } catch (e: IOException) {
            Result.failure(Exception("TreeUtil.getTree(): fatal: pathspec `$treeHash` did not match any files"))
        } catch (e: NoSuchFileException) {
            Result.failure(Exception("TreeUtil.getTree(): error could not find such a file"))
        } catch (e: Exception) {
            Result.failure(Exception("TreeUtil.getTree(): unknown error"))
        }
    }
}