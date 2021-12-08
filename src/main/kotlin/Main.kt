import java.io.File

fun main(args: Array<String>) {
    PrintBuilder()
        .setJustification(PrintBuilder.Justification.CENTER)
        .defineBitImage(BitImage.readFromBitmapFile(File(args[0])))
        .printBitImage(PrintBuilder.BitImagePrintMode.QUADRUPLE)
        .cut()
        .executePrint(File("/dev/ttyUSB0"))
}
