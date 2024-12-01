import org.junit.jupiter.api.Test
import util.Hashing.generateHash
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HashingTest {
    @Test
    fun `test generateHash with a sample string`() {
        // Should correctly hash the string 'hello'
        val content = "hello"
        val expectedHash = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"

        val generatedHash = generateHash(content)

        assertEquals(expectedHash, generatedHash)
    }

    @Test
    fun `test generateHash with a long string`() {
        val content = "fuata is the newest version control system in the software world"
        val expectedHash = "f810af8b13de347538d0162fde37eed0d66b54df"

        val generatedHash = generateHash(content)

        assertEquals(expectedHash, generatedHash)
    }

    @Test
    fun `test generateHash returns a different hash for the slightest change`() {
        val v1Content = "hello"
        val v1ExpectedHash = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"
        val v2Content = "hello."
        val v2ExpectedHash = "19bd4372b8ff2951d98be504333a88e35c9f8d0b"

        val v1Generated = generateHash(v1Content)
        val v2Generated = generateHash(v2Content)

        assertEquals(v1ExpectedHash, v1Generated)
        assertEquals(v2ExpectedHash, v2Generated)
        assertNotEquals(v1Generated, v2Generated)
    }
}