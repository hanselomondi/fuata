import org.junit.jupiter.api.Test
import util.Compression
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CompressionTest {

    @Test
    fun `compress and decompress functions map back to the same data`() {
        val fileContent = "This is content from the file test.txt"
        val compressedData = Compression.compressData(fileContent)
        println("Compressed data: $compressedData")
        val decompressedData = Compression.decompressData(compressedData)
        println("Decompressed data: $decompressedData")

        assertEquals(
            expected = fileContent,
            actual = decompressedData,
            message = "Decompressed data should map back to the original string"
        )
    }

    @Test
    fun `compressed data should be different for different strings`() {
        val file1Content = "The golden sun dipped below the tranquil horizon, casting a warm glow over the " +
                "shimmering ocean. Gentle waves lapped the sandy shore as soft whispers of the evening breeze carried" +
                " the scent of salt and wildflowers. Stars emerged, twinkling brightly, promising a peaceful night " +
                "under the vast, boundless heavens"
        val file2Content = "Beneath the towering forest canopy, vibrant rays of sunlight pierced through emerald " +
                "leaves, creating patterns on the moss-covered ground. Birds sang harmonious melodies as squirrels " +
                "darted playfully among ancient roots. A cool stream gurgled nearby, its crystal-clear waters " +
                "reflecting the serene beauty of nature’s timeless, ever-renewing splendor. Peace reigned " +
                "uninterrupted."
        val file1Compressed = Compression.compressData(file1Content)
        println("file1 Compressed: $file1Compressed")
        val file2Compressed = Compression.compressData(file2Content)
        println("file2 Compressed: $file2Compressed")

        assertNotEquals(
            file1Compressed,
            file2Compressed,
            "The compressed output should be different for two different files"
        )
    }
}