package util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Blob
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths

object Blob {

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
            println(compressedBlob)

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