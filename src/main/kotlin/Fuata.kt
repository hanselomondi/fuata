import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.IndexEntry
import util.Branching
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
    fun initialiseRepository() {
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
        } catch (e: Exception) {
            println(e.message ?: "failed initialisation : unknown error")
        }
    }

    /**
     * Invoked on the command `fuata add <filename>`
     * Adds a file to the staging area
     * @param path File path of the file being staged
     */
    fun add(path: String) {
        try {
            requireInitialisation("add")
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

    /**
     * Invoked on the command `fuata commit <commit_message>`
     * Creates a commit from the files that are currently staged
     * @param commitMessage The commit message
     */
    fun commit(
        commitMessage: String
    ) {
        try {
            requireInitialisation("commit")
            val fuataDir = repoDir.resolve(REPO_PROPER)
            val refsDir = fuataDir.resolve("$REFS_DIR/heads/main")
            val parentCommitHash = Files.readString(refsDir)
            // Read content from the index file
            val indexFileContent = Files.readAllBytes(fuataDir.resolve(INDEX_FILE))
            // In line 50 of Indexing.addFileToIndex, data is compressed before being written to the index file
            val decompressedFileContent = Compression.decompressData(indexFileContent)
            println("Fuata.commit() : content from index file: $decompressedFileContent")
            // Deserialize the content extracted from the index file
            val indexEntries = Json.decodeFromString<List<IndexEntry>>(decompressedFileContent)
            // Extract the index file content into a map of Map<filePath, fileHash>
            val stagedFiles = indexEntries.associate { indexEntry ->
                indexEntry.path to indexEntry.hash
            }
            println("stageFiles: Map<filePath, fileHash> = $stagedFiles")
            // Invoke the createCommit function
            CommitUtil.createCommit(
                message = commitMessage,
                parentCommitHash = parentCommitHash,
                stagedFiles = stagedFiles,
                objectsDirectory = fuataDir.resolve(OBJECTS_DIR).toString(),
                refsDirectory = fuataDir.resolve(refsDir).toString(),
                indexFile = fuataDir.resolve(INDEX_FILE).toString()
            ).also { println("newCommitHash: $it") }
        } catch (e: Exception) {
            println("Fuata.commit() : error while committing files: ${e.message ?: ""}")
        }
    }

    /**
     * Invoked on the command `<fuata log>`
     * Displays the full commit history of the repository
     */
    fun log() {
        try {
            requireInitialisation("log")
            val fuataDir = repoDir.resolve(REPO_PROPER)
            val head = fuataDir.resolve(HEAD_FILE)
            val objectsDirectory = fuataDir.resolve(OBJECTS_DIR)
            CommitUtil.logCommitHistory(
                head = head.toString(),
                objectsDirectory = objectsDirectory.toString()
            )
        } catch (e: Exception) {
            println("log : error: ${e.message ?: "unknown error while displaying log"}")
        }
    }

    /**
     * Invoked on the command `fuata create-branch <branch_name>`
     * Creates a new branch and points HEAD to it
     * @param branchName Name of the new branch being created
     */
    fun createBranch(branchName: String) {
        try {
            requireInitialisation("create-branch")
            val fuataDir = repoDir.resolve(REPO_PROPER)
            val head = fuataDir.resolve(HEAD_FILE)
            val refsDir = fuataDir.resolve(REFS_DIR)
            Branching.createBranch(
                branchName = branchName,
                head = head.toString(),
                refsDirectory = refsDir.toString(),
                repoProper = fuataDir.toString()
            ).fold(onSuccess = { println(it) }, onFailure = { throw it })
        } catch (e: Exception) {
            println("create-branch : error: ${e.message ?: "unknown error while creating branch"}")
        }
    }

    /**
     * Invoked on the command `fuata checkout <branch_name>`
     * Moves HEAD to reference the branch
     * @param branchName Name of the branch you want HEAD to point to
     */
    fun checkout(branchName: String) {
        try {
            requireInitialisation("checkout")
            val fuataDir = repoDir.resolve(REPO_PROPER)
            val refsDir = fuataDir.resolve(REFS_DIR)
            val head = fuataDir.resolve(HEAD_FILE)
            Branching.checkout(
                branchName = branchName,
                head = head.toString(),
                refsDirectory = refsDir.toString()
            ).fold(onSuccess = { println(it) }, onFailure = { throw it })
        } catch (e: Exception) {
            println("checkout : error: ${e.message ?: "unknown error"}")
        }
    }

    /**
     * Invoked on the command `fuata delete-branch <branch_name>`
     * Deletes the branch file from `.fuata/refs/heads/` directory
     * @param branchName Name of the branch being deleted
     */
    fun deleteBranch(branchName: String) {
        try {
            requireInitialisation("delete-branch")
            val fuataDir = repoDir.resolve(REPO_PROPER)
            val head = fuataDir.resolve(HEAD_FILE)
            val refsDir = fuataDir.resolve(REFS_DIR)
            Branching.deleteBranch(
                branchName = branchName,
                head = head.toString(),
                refsDirectory = refsDir.toString()
            ).fold(onSuccess = { println(it) }, onFailure = { throw it })
        } catch (e: Exception) {
            println("delete-branch : error: ${e.message ?: "unknown error"}")
        }
    }

    private companion object {
        const val REPO_DIR = "."  // Current working directory
        const val REPO_PROPER = ".fuata"
        const val OBJECTS_DIR = "objects"
        const val INDEX_FILE = "index"
        const val REFS_DIR = "refs"
        const val HEAD_FILE = "HEAD"

        /**
         * Checks if a Fuata repository has already been initialised in the current directory
         */
        fun repoIsInitialised(): Boolean {
            return Files.exists(Paths.get(REPO_DIR).resolve(REPO_PROPER))
        }

        /**
         * Checks if a Fuata repository has been initialised in the current directory before
         * running any command other than `fuata init`
         * @param command The particular command passed in the command line interface
         * @throws Exception When a Fuata repository has not been initialised in the current directory
         */
        fun requireInitialisation(command: String) {
            if (!repoIsInitialised())
                throw Exception("Fuata repository not found : cannot run command `fuata $command`")
        }
    }
}