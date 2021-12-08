import java.io.File

class PrintBuilder {

    enum class Justification {LEFT, CENTER, RIGHT}
    enum class BitImagePrintMode {NORMAL, DOUBLE_WIDTH, DOUBLE_HEIGHT, QUADRUPLE}

    private val printSequences = mutableListOf<Byte>()

    fun executePrint(printer: File) {
        printer.writeBytes(printSequences.toByteArray())
    }

    fun setFontScale(height: Int, width: Int): PrintBuilder {
        if (height !in 1..8 || width !in 1..8)
            error("Scales must be in range 1..8")

        printSequences.addAll(listOf(0x1D, 0x21, ((height - 1) + 0x10 * (width - 1)).toByte()))

        return this
    }

    fun printText(text: String): PrintBuilder {
        printSequences.addAll(text.chars().mapToObj { it.toByte() }.toList())

        return this
    }

    fun setJustification(justification: Justification): PrintBuilder {
        val justificationByte: Byte = when (justification) {
            Justification.LEFT -> 0x00
            Justification.CENTER -> 0x01
            Justification.RIGHT -> 0x02
        }
        printSequences.addAll(listOf(0x1B, 0x61, justificationByte))

        return this
    }

    fun defineBitImage(bitImage: BitImage): PrintBuilder {
        printSequences.addAll(bitImage.getImage())

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

    fun cut(feedLines: Int = 0xFF): PrintBuilder {
        printSequences.addAll(listOf(0x1D, 0x56, 0x42, feedLines.toByte()))

        return this
    }
}