import java.io.File
import kotlin.experimental.inv
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.pow

/**
 * This class represents a printable bit image with a total resolution of
 * `xSize * ySize * 8`. `imageData` is ordered column first, so the first column of
 * a bit image with a resolution of 1x1 would be exactly one byte (8 dots) and the
 * complete image would be 8 bytes big.
 * @param xSize     amount of pixels in horizontal direction divided by 8
 * @param ySize     amount of pixels in vertical direction divided by 8
 * @param imageData a list of bytes that define the image
 */
data class BitImage(val xSize: Int, val ySize: Int, val imageData: List<Byte>)

/**
 * Builds a printable bit image from a given bitmap image.
 *
 * The bitmap (windows bitmap, .bmp) must be monochromatic and uncompressed or
 * else errors will be thrown. Bit images are limited to a certain resolution
 * (see specification for details), depending on whether the image will be
 * defined in the volatile or non-volatile (NV) storage of the printer.
 * @param file the file to read the bitmap from
 * @return a printable bit image
 */
fun readFromBitmapFile(file: File): BitImage {
    val fileBytes = file.readBytes()

    fun read(offset: Int, length: Int = 4) = fileBytes.copyOfRange(offset, offset + length)
    fun ByteArray.decodeToInt() = foldIndexed(0) { index, acc, byte ->
        acc + 256.0.pow(index.toDouble()).toInt() * (byte.toInt() and 0xFF)
    }
    fun Int.makeDivisibleBy(n: Int) = this + (n - this % n) % n

    // check magic number
    if (!read(0, 2).contentEquals(byteArrayOf(0x42, 0x4D)))
        error("File is not a Windows Bitmap")
    // check color depth
    if (read(28, 2).decodeToInt() != 1)
        error("Bitmap must have a color depth of exactly 1")
    // check compression
    if (read(30).decodeToInt() != 0)
        error("Bitmap must be uncompressed")

    val imageDataOffset = read(10).decodeToInt()
    val bitmapWidth = read(18).decodeToInt()
    val bitmapHeight = read(22).decodeToInt()
    val imageData = read(imageDataOffset, fileBytes.size - imageDataOffset)
        .toList()
        .chunked(ceil(bitmapWidth / 8.0).toInt().makeDivisibleBy(4)) // padded to be divisible by 4
        .let {
            val list = if (bitmapHeight > 0) it.asReversed().toMutableList() else it.toMutableList()

            for (i in list.size until list.size.makeDivisibleBy(8))
                list.add(List(list.first().size) { 0xFF.toByte() })

            list
        }
        .map { it.take(bitmapWidth.makeDivisibleBy(8) / 8) }
        .chunked(8) { list ->
            List(list.first().size * 8) {
                var byte: Byte = 0

                for (i in 0 until 8) {
                    val bitSet = list[i][it / 8].toInt() and (128 shr it % 8) != 0
                    byte = byte.plus(if (bitSet) 128 shr i else 0).toByte()
                }

                byte
            }
        }
        .let { list ->
            // TODO: parse color table and only invert accordingly
            List(list.size * list.first().size) { list[it % list.size][it / list.size].inv() }
        }

    return BitImage(
        bitmapWidth.makeDivisibleBy(8) / 8,
        bitmapHeight.absoluteValue.makeDivisibleBy(8) / 8,
        imageData
    )
}
