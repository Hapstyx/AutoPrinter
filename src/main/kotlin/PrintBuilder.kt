import java.io.File

@Suppress("unused")
class PrintBuilder {

    enum class Justification {LEFT, CENTER, RIGHT}
    enum class BitImagePrintMode {NORMAL, DOUBLE_WIDTH, DOUBLE_HEIGHT, QUADRUPLE}

    /** Holds the sequences that will be sent to the printer */
    private val printSequences = mutableListOf<Byte>()
    /** Whether inverted printing is currently active or not */
    private var invertedPrintingMode = false
    /** Whether upside down printing is currently active or not */
    private var upsideDownPrintingMode = false

    /**
     * Sends all currently saved sequences to the printer. This does _NOT_ empty the queue.
     * @param printer the file (or device) to send the data to
     * @see reset
     */
    fun executePrint(printer: File) = printer.writeBytes(printSequences.toByteArray())

    /**
     * Clears all saved sequences.
     */
    fun reset() = printSequences.clear()

    /**
     * Adds the specified sequence to the end of the queue.
     * @param byteSequence the sequence to append to the queue
     * @return this builder instance
     */
    fun addCustomSequence(byteSequence: Iterable<Byte>) = printSequences.addAll(byteSequence).let { this }

    /**
     * Adds the specified sequence to the end of the queue.
     * @param byteSequence the sequence to append to the queue
     * @return this builder instance
     */
    fun addCustomSequence(byteSequence: ByteArray) = addCustomSequence(byteSequence.asList())

    /**
     * Feeds paper and cuts it.
     * @param feedLines how many dots to feed (must be in 0..255, default 255)
     * @return this builder instance
     */
    fun cut(feedLines: Int = 0xFF) = printSequences.addAll(0x1D, 0x56, 0x42, feedLines.toByte()).let { this }

    /**
     * Sets the scale of printed characters.
     * @param height how much to scale a character vertically (normal 1, max 8)
     * @param width  how much to scale a character horizontally (normal 1, max 8)
     * @return this builder instance
     */
    fun setFontScale(height: Int, width: Int) =
        printSequences.addAll(0x1D, 0x21, ((height - 1) + 0x10 * (width - 1)).toByte())
            .let { this }

    /**
     * Prints the given string. ASCII characters will always be printed correctly, however,
     * the printer only supports certain encodings for the latin alphabet. Refer to
     * [CodePage] for a list of all currently supported encodings. CP-1252 is assumed as the
     * default encoding. Some escape sequences which translate directly to an ASCII control
     * sequence such as `\n` are supported as well.
     * @param text     the text to append to the queue
     * @param codePage the code page to use for decoding
     * @return this builder instance
     */
    fun printText(text: String, codePage: CodePage = CodePage.CP1252): PrintBuilder {
        val codeTable = when (codePage) {
            CodePage.CP437  ->  0
            CodePage.CP850  ->  2
            CodePage.CP860  ->  3
            CodePage.CP863  ->  4
            CodePage.CP865  ->  5
            CodePage.CP1252 -> 16
            CodePage.CP866  -> 17
            CodePage.CP852  -> 18
            CodePage.CP858  -> 19
        }

        printSequences.addAll(0x1B, 0x74, codeTable.toByte())
        printSequences.addAll(decodeWithCodeTable(text, codePage))
        printSequences.addAll(0x1B, 0x64, 0x00)

        return this
    }

    /**
     * Toggles the inverted printing mode. All dots that were printed as white will be printed
     * as black and vice versa.
     * @return this builder instance
     */
    fun toggleInvertedPrintingMode() =
        printSequences.addAll(0x1D, 0x42, if (!invertedPrintingMode) 0x01 else 0x00)
            .also { invertedPrintingMode = !invertedPrintingMode }
            .let { this }

    /**
     * Toggles the upside down printing mode. All lines will be rotated by 180° and then printed.
     * @return this builder instance
     */
    fun toggleUpsideDownPrintingMode() =
        printSequences.addAll(0x1B, 0x7B, if (!upsideDownPrintingMode) 0x01 else 0x00)
            .also { upsideDownPrintingMode = !upsideDownPrintingMode }
            .let { this }

    /**
     * Set the justification to one of three possibilities (normal LEFT).
     * @param justification the justification to use
     * @return this builder instance
     */
    fun setJustification(justification: Justification): PrintBuilder {
        val justificationByte: Byte = when (justification) {
            Justification.LEFT -> 0x00
            Justification.CENTER -> 0x01
            Justification.RIGHT -> 0x02
        }
        printSequences.addAll(0x1B, 0x61, justificationByte)

        return this
    }

    /**
     * Defines a bit image in non-volatile (NV) storage. This does _NOT_ print the image.
     * @param imageNumber the image to define (min 1, max 255)
     * @param bitImage    the bit image data
     * @return this builder instance
     */
    fun defineNVBitImage(imageNumber: Int, bitImage: BitImage): PrintBuilder {
        printSequences.addAll(0x1C, 0x71, imageNumber.toByte())
        printSequences.addAll((bitImage.xSize % 0xFF).toByte(), (bitImage.xSize / 0xFF).toByte())
        printSequences.addAll((bitImage.ySize % 0xFF).toByte(), (bitImage.ySize / 0xFF).toByte())
        printSequences.addAll(bitImage.imageData)

        return this
    }

    /**
     * Prints a defined bit image from non-volatile (NV) storage.
     * @param imageNumber       the image to print (min 1, max 255)
     * @param bitImagePrintMode if and how to print / stretch the image
     * @return this builder instance
     */
    fun printNVBitImage(imageNumber: Int, bitImagePrintMode: BitImagePrintMode): PrintBuilder {
        val bitImagePrintModeByte: Byte = when (bitImagePrintMode) {
            BitImagePrintMode.NORMAL -> 0x00
            BitImagePrintMode.DOUBLE_WIDTH -> 0x01
            BitImagePrintMode.DOUBLE_HEIGHT -> 0x02
            BitImagePrintMode.QUADRUPLE -> 0x03
        }
        printSequences.addAll(0x1C, 0x70, imageNumber.toByte(), bitImagePrintModeByte)

        return this
    }

    /**
     * Defines a bit image in volatile storage. This does _NOT_ print the image.
     * @param bitImage the bit image data
     * @return this builder instance
     */
    fun defineBitImage(bitImage: BitImage): PrintBuilder {
        printSequences.addAll(0x1D, 0x2A, bitImage.xSize.toByte(), bitImage.ySize.toByte())
        printSequences.addAll(bitImage.imageData)

        return this
    }

    /**
     * Prints a defined bit image from volatile storage.
     * @param bitImagePrintMode if and how to print / stretch the image
     * @return this builder instance
     */
    fun printBitImage(bitImagePrintMode: BitImagePrintMode): PrintBuilder {
        val bitImagePrintModeByte: Byte = when (bitImagePrintMode) {
            BitImagePrintMode.NORMAL -> 0x00
            BitImagePrintMode.DOUBLE_WIDTH -> 0x01
            BitImagePrintMode.DOUBLE_HEIGHT -> 0x02
            BitImagePrintMode.QUADRUPLE -> 0x03
        }
        printSequences.addAll(0x1D, 0x2F, bitImagePrintModeByte)

        return this
    }

    /**
     * Adds all given elements to the end of this list.
     * @param elements the elements to append to the end of the list
     */
    private fun MutableList<Byte>.addAll(vararg elements: Byte) = addAll(elements.asList())
}
