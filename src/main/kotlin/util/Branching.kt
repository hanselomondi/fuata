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
     * @param head File path to `.fuata/HEAD`
     * @param refsDirectory Files path to fuata refs directory `.fuata/refs/`
     * @return The success message in a Result wrapper
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
     * @param head File path to `.fuata/HEAD`
     * @param refsDirectory Files path to fuata refs directory `.fuata/refs/`
     * @return The success message wrapped in a Result
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

    /**
     * Deletes a branch from the repository
     * Deletes the branch file in `.fuata/refs/heads/`
     * @param branchName Name of the branch to delete
     * @param head File path to `.fuata/HEAD`
     * @param refsDirectory File path to fuata refs directory `.fuata/refs`
     * @return The success message wrapped in a Result
     * @throws FileNotFoundException If the branch does not exist
     * @throws Exception
     */
    fun deleteBranch(
        branchName: String,
        head: String,
        refsDirectory: String
    ): Result<String> {
        return try {
            val headRef = Files.readString(Paths.get(head))
            val branchPath = Paths.get(refsDirectory).resolve("heads/$branchName")
            // Check if the branch exists
            if (Files.exists(branchPath)) {
                // Check if the current branch is what HEAD is pointing to
                if ("refs/heads/$branchName" == headRef)
                    throw Exception("error deleting branch: currently checked out on branch '$branchName'")
                // Delete the file in refs directory
                Files.delete(branchPath)
                // Return the Success message
                Result.success("deleted branch '$branchName'")
            } else {
                throw FileNotFoundException("refs/heads/$branchName")
            }
            Result.success("deleted the branch '$branchName'")
        } catch (e: FileNotFoundException) {
            Result.failure(Exception("fatal: branch does not exist: cannot delete branch '$branchName'"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "unknown error"))
        }
    }
}