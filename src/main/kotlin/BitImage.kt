import java.io.File
import kotlin.experimental.inv
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.pow

@Suppress("MemberVisibilityCanBePrivate")
class BitImage {

    val xSize: Int
    val ySize: Int
    val imageHeader: List<Byte>
    val imageData: List<Byte>

    constructor(xSize: Int, ySize: Int, imageData: List<Byte> = List(xSize * ySize * 8) {0}) {
        if (xSize !in 1..255)
            error("xSize must be in range 1..255")
        if (ySize !in 1..48)
            error("ySize must be in range 1..48")
        if (xSize * ySize > 1_536)
            error("xSize * ySize must be less than or equal to 1536")

        this.xSize = xSize
        this.ySize = ySize
        this.imageHeader = listOf(0x1D, 0x2A, xSize.toByte(), ySize.toByte())
        this.imageData = imageData
    }

    // what was the point of this again?
    constructor(dotMatrix: List<List<Boolean>>, MSBFirst: Boolean = true) {
        if (dotMatrix.size !in 1..255)
            error("Number of columns must be in range 1..255")
        if (dotMatrix.first().size / 8 !in 1..48)
            error("Number of rows must be in range 1..48")
        if (dotMatrix.size * (dotMatrix.first().size / 8) > 1_536)
            error("Number of columns times number of rows must be less than or equal to 1536")

        fun twoPow(x: Int) = 2.0.pow(x.toDouble()).toInt()

        this.xSize = dotMatrix.size
        this.ySize = dotMatrix.first().size / 8
        this.imageHeader = listOf(0x1D, 0x2A, xSize.toByte(), ySize.toByte())
        this.imageData = List(xSize * ySize) {
            dotMatrix[it / ySize]
                .subList((it * 8) % (ySize * 8), (it * 8) % (ySize * 8) + 8)
                .let { list -> if (MSBFirst) list.asReversed() else list }
                .foldIndexed(0) { index, acc, b -> acc + (if (b) twoPow(index) else 0) }
                .toByte()
        }
    }

    companion object Companion {
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
                .map { list -> list
                    .take(bitmapWidth.makeDivisibleBy(8) / 8)
                    .flatMap { byte -> List(8) { (128 shr it) and byte.toInt() != 0 } }
                }
                .chunked(8)
                .map { list ->
                    List(list.first().size) {
                        var byte: Byte = 0

                        for (i in 0 until 8)
                            byte = byte.plus(if (list[i][it]) 128 shr i else 0).toByte()

                        byte
                    }
                }
                .let { list ->
                    List(list.size * list.first().size) { list[it % list.size][it / list.size].inv() }
                }

            return BitImage(bitmapWidth.makeDivisibleBy(8) / 8, bitmapHeight.absoluteValue.makeDivisibleBy(8) / 8, imageData)
        }
    }

    fun getImage() = imageHeader + imageData

    fun getDotRow(rowNumber: Int): List<Boolean> {
        if (rowNumber !in 0 until (ySize * 8))
            error("rowNumber must be in range ${0 until (ySize * 8)}, but was $rowNumber")

        return imageData
            .filterIndexed { index, _ -> index % ySize == rowNumber / 8 }
            .map { byte -> byte.toInt() and (1 shl rowNumber) != 0 }
    }

    fun getDotColumn(columnNumber: Int): List<Boolean> {
        if (columnNumber !in 0 until (xSize * 8))
            error("columnNumber must be in range ${0 until (xSize * 8)}, but was $columnNumber")

        return imageData
            .filterIndexed { index, _ -> index / ySize == columnNumber }
            .flatMap { byte -> List(8) { index -> (byte.toInt() and (128 shr index)) != 0 } }
    }

    fun getDotMatrix(): List<List<Boolean>> {
        return imageData
            .chunked(ySize)
            .map { it.flatMap { byte -> List(8) { index -> (byte.toInt() and (128 shr index)) != 0 } } }
    }
}
