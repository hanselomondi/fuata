import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import util.Compression
import util.Indexing
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexingTest {

    @TempDir
    private lateinit var testDir: Path
    private lateinit var objectDir: Path
    private lateinit var index: Path

    @BeforeEach
    fun setUp() {
        objectDir = testDir.resolve("objects")
        index = testDir.resolve("index")
        Files.createDirectory(objectDir)
    }

    @AfterEach
    fun cleanUp() {
        testDir.toFile().deleteRecursively()
    }

    @Test
    fun `addFileToIndex correctly adds a file being staged to the index file`() {
        val testFile = testDir.resolve("test.txt")
        Files.writeString(testFile, "This is a test file")
        val result = Indexing.addFileToIndex(
            filePath = testFile.toString(),
            objectDirectory = objectDir.toString(),
            indexFile = index.toString()
        )

        val indexFileContent = Compression.decompressData(Files.readAllBytes(index))
        println("Index file content: $indexFileContent")
        assertTrue(indexFileContent.isNotEmpty())
        assertTrue(result.isSuccess, "The addition of the file to the index should be successful")
        val hash = result.getOrNull() ?: throw AssertionError("Hash should not be null")
        val fileBlob = objectDir.resolve(hash)
        assertTrue(Files.exists(fileBlob), "The staged file should have a blob created")
    }

    @Test
    fun `addFileToIndex fails when an incorrect filePath is provided`() {
        val result = Indexing.addFileToIndex(
            filePath = "file2.txt",
            objectDirectory = objectDir.toString(),
            indexFile = index.toString()
        )

        println("error message ${result.exceptionOrNull()?.message}")
        assertTrue(result.isFailure)
        assertEquals(
            expected = "fatal: pathspec `file2.txt` did not match any files",
            actual = result.exceptionOrNull()?.message,
            message = "Indexing operation fails when the file path is incorrect"
        )
    }

    @Test
    fun `index should have content when addFileToIndex is performed multiple times`() {
        val file1Path = testDir.resolve("file1.txt")
        Files.writeString(file1Path, "File 1 content for the unit test")
        var result = Indexing.addFileToIndex(
            filePath = file1Path.toString(),
            objectDirectory = objectDir.toString(),
            indexFile = index.toString()
        )
        assertTrue(result.isSuccess)
        // Content in the index file after the first file is staged
        val indexV1 = Compression.decompressData(Files.readAllBytes(index))
        println("IndexV1: $indexV1")

        // Index second file
        val file2Path = testDir.resolve("file2.kt")
        Files.writeString(file2Path, "fn main() { println(\"Hello, World!\") }")
        result = Indexing.addFileToIndex(
            filePath = file2Path.toString(),
            objectDirectory = objectDir.toString(),
            indexFile = index.toString()
        )
        assertTrue(result.isSuccess)
        // Content in the index file after the second file is staged
        val indexV2 = Compression.decompressData(Files.readAllBytes(index))
        println("IndexV2: $indexV2")
        assertFalse(indexV1 == indexV2, "Content after the two separate operations should not be equal")
    }
}