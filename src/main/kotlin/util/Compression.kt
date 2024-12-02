package util

import java.util.zip.Deflater
import java.util.zip.Inflater

object Compression {

    fun compressData(data: String): ByteArray {
        val deflater = Deflater()
        deflater.setInput(data.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val output = ByteArray(1024)  // Buffer to be filled with the compressed data
        println("Output: $output")
        val compressedSize = deflater.deflate(output)
        return output.copyOf(compressedSize)
    }

    fun decompressData(data: ByteArray): String {
        val inflater = Inflater()
        inflater.setInput(data)
        val output = ByteArray(1024)  // Buffer to be filed with the decompressed data
        val decompressedSize = inflater.inflate(output)
        return String(output, 0, decompressedSize, Charsets.UTF_8)
    }
}