import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Fuata(repoDir: Path = Paths.get(REPO_DIR).toAbsolutePath().normalize()) {

    /**
     * Initialises a repository proper `.fuata` that will store all version history information
     * @throws FileAlreadyExistsException If a Fuata directory was already initialise in the directory
     */
    init {
        try {
//            val currentDir = Paths.get(REPO_DIR).toAbsolutePath().normalize()
            val fuataDir = repoDir.resolve(".fuata")
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

            val objectsDir = fuataDir.resolve("objects")
            Files.createDirectory(objectsDir)
            val refsDir = fuataDir.resolve("refs")
            Files.createDirectory(refsDir)
            val headPath = fuataDir.resolve("HEAD")
            Files.createFile(headPath)
            val indexPath = fuataDir.resolve("index")
            Files.createFile(indexPath)
            println("Initialised a Fuata directory in ${Paths.get(fuataDir.toString()).toAbsolutePath()}")
        } catch (e: FileAlreadyExistsException) {
            println(e.reason)
            throw e
        }
    }

    /**
     * Invoked on the command `fuata add <filename>`
     * Adds a file to the staging area
     */
    fun add() {}

    private companion object {
        const val REPO_DIR = "."  // Current working directory
    }
}