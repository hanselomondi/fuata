package util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Blob
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths

object Blob {

    /**
     * Creates a new [blob][Blob] object and adds it to a file in .fuata/objects/ directory
     * This blob file will have the SHA-1 of the content as its name and return this hash value
     * @param filePath File path of the file in the working directory whose blob is being created
     * @param objectsFolderPath File path of .fuata/objects/ directory where the blob file will be stored
     * @return [Result] object with a string hash on success otherwise an exception on failure
     * @throws InvalidPathException when an invalid filepath argument is passed
     * @throws IOException when an invalid filepath argument is passed
     * @throws Exception for any other operation that may fail
     */
    fun createBlob(filePath: String, objectsFolderPath: String): Result<String> {
        return try {
            val path = Paths.get(filePath)
            println("file path: $path")
            val fileContent = Files.readString(path)
            val hash = Hashing.generateHash(fileContent)

            // Create Blob meta data
            val blob = Blob(
                fileContent = fileContent,
                path = filePath
            )
            val blobJson = Json.encodeToString(value = blob)  // Converts Blob object to a JSON string
            println(blobJson)
            val compressedBlob = Compression.compressData(blobJson)
            println("Compressed blob: $compressedBlob")

            // Write data to objects file
            val objectsPath = Paths.get(objectsFolderPath, hash)
            Files.write(objectsPath, compressedBlob)  // Write the byte array blob in the file

            Result.success(hash)
        } catch (e: InvalidPathException) {
            Result.failure(Exception("fatal: pathspec `$filePath` did not match any files"))
        } catch (e: IOException) {
            Result.failure(Exception("fatal: pathspec `$filePath` did not match any files"))
        } catch (e: Exception) {
            Result.failure(Exception("unknown error: ${e.message}"))
        }
    }
}