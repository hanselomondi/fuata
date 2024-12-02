import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuataInitTest {

    private lateinit var testDir: Path

    @BeforeEach
    fun setup() {
        testDir = Files.createTempDirectory("fuata_test")
        System.setProperty(
            "user.dir",
            testDir.toString()
        )  // Mock the current directory so that it doesn't affect the actual working directory
        println("Current directory for test: ${System.getProperty("user.dir")}")
    }

    @AfterEach
    fun cleanUp() {
        testDir.toFile().deleteRecursively()
    }

    @Test
    fun `init should create fuata directory and related files`() {
        Fuata(testDir) // Initialise directory
        val fuataDir = testDir.resolve(".fuata").normalize()
        assertTrue(Files.exists(fuataDir), "repository proper exists in the current working directory")
        assertTrue(Files.exists(fuataDir.resolve("objects")), "objects directory has been created within .fuata")
        assertTrue(Files.exists(fuataDir.resolve("HEAD")), "HEAD file has been created in .fuata")
        assertTrue(Files.exists(fuataDir.resolve("refs")), "refs directory has been created in .fuata")
        assertTrue(Files.exists(fuataDir.resolve("index")), "index file has been created in .fuata")
    }

    @Test
    fun `init should throw an error if fuata proper is already initialised`() {
        Fuata(testDir)  // Initialise directory

        val exception = assertThrows<FileAlreadyExistsException> {
            Fuata(testDir)  // Attempt to reinitialise the directory
        }

        assertEquals(
            expected = "Current directory has already been initialised as a fuata directory",
            actual = exception.reason,
            message = "Ensure the exception displays the correct message"
        )
    }
}