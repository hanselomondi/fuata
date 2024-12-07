package util

import java.io.FileNotFoundException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Paths

object Branching {

    /**
     * Creates a new branch in the repository
     * Creates a new file in `.fuata/refs/heads/`
     * Changes the branch HEAD directly references
     * @param branchName Name of the new branch being created
     * @param head File path of `.fuata/HEAD`
     * @param refsDirectory Files path of fuata refs directory `.fuata/refs/`
     * @return The new branch name in a Result wrapper
     * @throws FileAlreadyExistsException If the branch already exists
     * @throws Exception
     */
    fun createBranch(
        branchName: String,
        head: String,
        refsDirectory: String,
        repoProper: String
    ): Result<String> {
        return try {
            // Check if a branch by the same name already exists
            if (!Files.exists(Paths.get(refsDirectory).resolve("heads/$branchName"))) {
                val headFile = Paths.get(head)
                // Read current branch being referenced by HEAD
                val currentRef = Files.readString(headFile)
                // Read the commit hash at the branch
                val headCommitHash = Files.readString(Paths.get(repoProper).resolve(currentRef))
                // Create a new file under refs/heads/
                val newBranchFile = Files.createFile(Paths.get(refsDirectory).resolve("heads/$branchName"))
                // Write the commit hash in the new file
                Files.writeString(newBranchFile, headCommitHash)
                // Write the path of the new file in HEAD
                Files.writeString(headFile, "refs/heads/$branchName")
                // Return the branch name
                Result.success("Switched to a new branch '$branchName'")
            } else {
                throw FileAlreadyExistsException("refs/heads/$branchName")
            }
        } catch (e: FileAlreadyExistsException) {
            Result.failure(Exception("fatal: $branchName already exists"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "unknown error"))
        }
    }

    /**
     * Moves the HEAD from one branch to another
     * @param branchName The name of the branch you want HEAD to point to
     * @param head File path of `.fuata/HEAD`
     * @param refsDirectory Files path of fuata refs directory `.fuata/refs/`
     * @return The branch name wrapped in a Result
     * @throws FileNotFoundException If no branch by the name provided exists
     * @throws Exception
     */
    fun checkout(
        branchName: String,
        head: String,
        refsDirectory: String
    ): Result<String> {
        return try {
            // Check if the branch exists
            if (Files.exists(Paths.get(refsDirectory).resolve("heads/$branchName"))) {
                // Update the reference in HEAD
                Files.writeString(Paths.get(head), "refs/heads/$branchName")
                // Return a Success and the branch name
                Result.success("Switched to branch '$branchName'")
            } else {
                throw FileNotFoundException(branchName)
            }
        } catch (e: FileNotFoundException) {
            Result.failure(Exception("fatal: branch does not exist: cannot checkout to $branchName"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "unknown error"))
        }
    }
}