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

        imageHeader = byteArrayOf(0x1D, 0x2A, xSize.toByte(), ySize.toByte())
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
