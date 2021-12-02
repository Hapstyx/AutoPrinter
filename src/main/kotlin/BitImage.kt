class BitImage(val xSize: Int, val ySize: Int, val imageData: List<Byte> = List(xSize * ySize * 8) {0}) {

    private val imageHeader: ByteArray

    init {
        if (xSize !in 1..255)
            error("xSize must be in range 1..255")
        if (ySize !in 1..48)
            error("ySize must be in range 1..48")

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
            .map { columnBytes ->
                columnBytes.flatMap { byte -> List(8) { index -> (byte.toInt() and (128 shr index)) != 0 } }
            }
    }
}
