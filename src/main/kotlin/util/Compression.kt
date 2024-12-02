package util

import java.util.zip.Deflater
import java.util.zip.Inflater

object Compression {

    /**
     * Converts a String into a ByteArray and compresses it for efficient file storage
     * @param data String data to be converted into a ByteArray
     * @return A ByteArray that has been compressed to occupy less space
     */
    fun compressData(data: String): ByteArray {
        val deflater = Deflater()
        deflater.setInput(data.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val output = ByteArray(1024)  // Buffer to be filled with the compressed data
        println("Output: $output")
        val compressedSize = deflater.deflate(output)
        return output.copyOf(compressedSize)
    }

    /**
     * Decompresses a ByteArray and converts it into its original String equivalent
     * @param data Compressed ByteArray to be converted
     * @return Original String that had been compressed
     */
    fun decompressData(data: ByteArray): String {
        val inflater = Inflater()
        inflater.setInput(data)
        val output = ByteArray(1024)  // Buffer to be filed with the decompressed data
        val decompressedSize = inflater.inflate(output)
        return String(output, 0, decompressedSize, Charsets.UTF_8)
    }
}