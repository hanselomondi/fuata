import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.IndexEntry
import util.CommitUtil
import util.Compression
import util.Indexing
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

class Fuata(private val repoDir: Path = Paths.get(REPO_DIR).toAbsolutePath().normalize()) {

    /**
     * Initialises a repository proper `.fuata` that will store all version history information
     * @throws FileAlreadyExistsException If a Fuata directory was already initialise in the directory
     */
    init {
        try {
            val fuataDir = repoDir.resolve(REPO_PROPER)
            println("fuataDir: $fuataDir")
            // Create repository proper
            if (!Files.exists(fuataDir)) {
                Files.createDirectory(fuataDir)
            } else {
                // If the directory has already been initialised as a fuata directory
                throw FileAlreadyExistsException(
                    File(".fuata"),
                    reason = "Current directory has already been initialised as a fuata directory"
                )
            }

            Files.createDirectory(fuataDir.resolve(OBJECTS_DIR))
            Files.createDirectory(fuataDir.resolve(REFS_DIR))
            Files.createDirectory(fuataDir.resolve("$REFS_DIR/heads"))
            Files.createFile(fuataDir.resolve("$REFS_DIR/heads/main"))
            Files.createFile(fuataDir.resolve(HEAD_FILE))
            Files.createFile(fuataDir.resolve(INDEX_FILE))
            // Write an initial main branch in the HEAD file
            Files.writeString(fuataDir.resolve(HEAD_FILE), "refs/heads/main")
            // Write an empty array to index file
            val emptyJsonList = Json.encodeToString(emptyList<String>())
            Files.writeString(fuataDir.resolve(INDEX_FILE), emptyJsonList)
            println("Initialised a Fuata directory in ${Paths.get(fuataDir.toString()).toAbsolutePath()}")
        } catch (e: FileAlreadyExistsException) {
            println(e.reason)
            throw e
        }
    }

    /**
     * Invoked on the command `fuata add <filename>`
     * Adds a file to the staging area
     * @param path File path of the file being staged
     */
    fun add(path: String) {
        try {
            val filePath = Paths.get(REPO_DIR, path)
            val fuataDir = repoDir.resolve(REPO_PROPER)
            val result = Indexing.addFileToIndex(
                filePath = filePath.toString(),
                objectDirectory = fuataDir.resolve(OBJECTS_DIR).toString(),
                indexFile = fuataDir.resolve(INDEX_FILE).toString()
            )
            result.onSuccess {
                println("success: added $path to staging index")
            }.onFailure {
                throw it
            }
        } catch (e: InvalidPathException) {
            println("fatal: pathspec `$path` did not match any files")
        } catch (e: IOException) {
            println("fatal: pathspec `$path` did not match any files")
        } catch (e: Exception) {
            println("fatal: failed to stage file : ${e.message ?: ""}")
        }
    }

    private companion object {
        const val REPO_DIR = "."  // Current working directory
        const val REPO_PROPER = ".fuata"
        const val OBJECTS_DIR = "objects"
        const val INDEX_FILE = "index"
        const val REFS_DIR = "refs"
        const val HEAD_FILE = "HEAD"
    }
}