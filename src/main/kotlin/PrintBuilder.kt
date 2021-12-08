import java.io.File

@Suppress("unused")
class PrintBuilder {

    enum class Justification {LEFT, CENTER, RIGHT}
    enum class BitImagePrintMode {NORMAL, DOUBLE_WIDTH, DOUBLE_HEIGHT, QUADRUPLE}

    private val printSequences = mutableListOf<Byte>()
    private var invertedPrintingMode = false

    fun executePrint(printer: File) = printer.writeBytes(printSequences.toByteArray()).let { this }

    fun reset() = printSequences.clear().let { this }

    fun addCustomSequence(byteSequence: Iterable<Byte>) = printSequences.addAll(byteSequence).let { this }

    fun addCustomSequence(byteSequence: ByteArray) = addCustomSequence(byteSequence.asList())

    fun cut(feedLines: Int = 0xFF) = printSequences.addAll(listOf(0x1D, 0x56, 0x42, feedLines.toByte())).let { this }

    fun setFontScale(height: Int, width: Int): PrintBuilder {
        if (height !in 1..8 || width !in 1..8)
            error("Scales must be in range 1..8")

        printSequences.addAll(listOf(0x1D, 0x21, ((height - 1) + 0x10 * (width - 1)).toByte()))

        return this
    }

    fun printText(text: String) = printSequences.addAll(text.chars().mapToObj { it.toByte() }.toList()).let { this }

    fun toggleInvertedPrintingMode() =
        printSequences.addAll(listOf(0x1D, 0x42, if (!invertedPrintingMode) 0x01 else 0x00))
            .also { invertedPrintingMode = !invertedPrintingMode }
            .let { this }

    fun setJustification(justification: Justification): PrintBuilder {
        val justificationByte: Byte = when (justification) {
            Justification.LEFT -> 0x00
            Justification.CENTER -> 0x01
            Justification.RIGHT -> 0x02
        }
        printSequences.addAll(listOf(0x1B, 0x61, justificationByte))

        return this
    }

    fun defineNVBitImage(imageNumber: Int, bitImage: BitImage): PrintBuilder {
        printSequences.addAll(listOf(0x1C, 0x71, imageNumber.toByte()))
        printSequences.addAll(listOf((bitImage.xSize % 0xFF).toByte(), (bitImage.xSize / 0xFF).toByte()))
        printSequences.addAll(listOf((bitImage.ySize % 0xFF).toByte(), (bitImage.ySize / 0xFF).toByte()))
        printSequences.addAll(bitImage.imageData)

        return this
    }

    fun printNVBitImage(imageNumber: Int, bitImagePrintMode: BitImagePrintMode): PrintBuilder {
        val bitImagePrintModeByte: Byte = when (bitImagePrintMode) {
            BitImagePrintMode.NORMAL -> 0x00
            BitImagePrintMode.DOUBLE_WIDTH -> 0x01
            BitImagePrintMode.DOUBLE_HEIGHT -> 0x02
            BitImagePrintMode.QUADRUPLE -> 0x03
        }
        printSequences.addAll(listOf(0x1C, 0x70, imageNumber.toByte(), bitImagePrintModeByte))

        return this
    }

    fun defineBitImage(bitImage: BitImage): PrintBuilder {
        printSequences.addAll(listOf(0x1D, 0x2A, bitImage.xSize.toByte(), bitImage.ySize.toByte()))
        printSequences.addAll(bitImage.imageData)

        return this
    }

    fun printBitImage(bitImagePrintMode: BitImagePrintMode): PrintBuilder {
        val bitImagePrintModeByte: Byte = when (bitImagePrintMode) {
            BitImagePrintMode.NORMAL -> 0x00
            BitImagePrintMode.DOUBLE_WIDTH -> 0x01
            BitImagePrintMode.DOUBLE_HEIGHT -> 0x02
            BitImagePrintMode.QUADRUPLE -> 0x03
        }
        printSequences.addAll(listOf(0x1D, 0x2F, bitImagePrintModeByte))

        return this
    }
}