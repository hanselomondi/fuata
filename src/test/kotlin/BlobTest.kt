import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import util.Blob
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlobTest {

    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `createBlob should successfully process a valid file`() {
        // Create a test file
        val testFile = tempDir.resolve("test.txt")
        Files.writeString(testFile, "This is a test file")

        val objectsDir = tempDir.resolve("objects")
        Files.createDirectory(objectsDir)

        val result = Blob.createBlob(
            filePath = testFile.toString(),
            objectsFolderPath = objectsDir.toString()
        )

        assertTrue(result.isSuccess, "Blob creation should be successful")
        val hash = result.getOrNull() ?: throw AssertionError("Hash should not be null")
        val blobFile = objectsDir.resolve(hash)
        assertTrue(Files.exists(blobFile))
    }

    @Test
    fun `createBlob should fail if the file does not exist`() {
        val objectsDir = tempDir.resolve("objects")
        Files.createDirectory(objectsDir)

        val result = Blob.createBlob("nonexistent.txt", objectsDir.toString())
        assertTrue(result.isFailure, "Blob creation should fail for non existent file")
        assertEquals(
            expected = "fatal: pathspec `nonexistent.txt` did not match any files",
            actual = result.exceptionOrNull()?.message,
            "Blob creation should return a Result.Failure"
        )
    }
}