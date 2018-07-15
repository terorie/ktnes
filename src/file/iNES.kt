package com.terorie.ktnes.file

import com.terorie.ktnes.ROMException
import com.terorie.ktnes.compareByteArrays
import com.terorie.ktnes.toUnsigned
import java.nio.file.Files
import java.nio.file.Paths

class iNESFile(pathString: String) {

    val path = Paths.get(pathString)
    val dump = Files.readAllBytes(path)

    //val prgRom: ByteArray

    init {
        // Begin Header

        // Magic number
        if (!compareByteArrays(dump, byteArrayOf(0x4E, 0x45, 0x53, 0x1A)))
            throw ROMException("Not a .NES file")

        // Size of PRG ROM
        val prgRomSize = 16384 * dump[4].toUnsigned()

        // Size of CHR ROM
        val chrRomSize = 8192 * dump[5].toUnsigned()

        // Flags 6
        //val flags6 =
    }

}
