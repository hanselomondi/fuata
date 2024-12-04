package util

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import model.IndexEntry
import java.nio.file.Files
import java.nio.file.Paths

object Indexing {

    /**
     * Updates the content of the index file in the repository proper with details of the file being staged, after
     * creating the file blob
     * Content written to a string is not compressed. Therefore, when reading from it, there is no decompression required
     * Files.writeString writes a [CharSequence] as bytes in .fuata/index, while Files.readString decodes the bytes into
     * a CharSequence
     * @param filePath Path of the file being staged and added to the index
     * @param objectDirectory Path to .fuata/objects/ directory
     * @param indexFile Path to .fuata/index file
     * @return [Result] that is a hash of the blob file, on success
     */
    @OptIn(InternalSerializationApi::class)
    fun addFileToIndex(
        filePath: String,
        objectDirectory: String,
        indexFile: String
    ): Result<String> {
        return try {
            val blobResult = Blob.createBlob(filePath, objectDirectory)
            blobResult.onSuccess { blobHash ->
                // Create an IndexEntry for the file being staged
                val indexEntry = IndexEntry(
                    path = filePath,
                    hash = blobHash
                )
                println(indexEntry)
                // Read existing data in the .fuata/index file
                var indexData = if (Files.exists(Paths.get(indexFile))) {
                    val existingData = Files.readString(Paths.get(indexFile))
                    println("existing index data: $existingData")
                    Json.decodeFromString<List<IndexEntry>>(existingData)
                } else {
                    mutableListOf()  // Return an empty list the file does not exist
                }

                indexData = indexData.plus(indexEntry)
                println(indexData)
                val compressedData = Compression.compressData(
                    Json.encodeToString(ListSerializer(IndexEntry::class.serializer()), indexData)
                        .also { println("Data written to index: $it") }  // Debugging info
                )
                Files.write(Paths.get(indexFile), compressedData)

                Result.success(blobHash)
            }.onFailure { exception ->
                throw exception
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message))
        }
    }
}