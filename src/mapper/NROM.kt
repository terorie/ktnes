package com.terorie.ktnes.mapper

import com.terorie.ktnes.ExtendedBytes
import com.terorie.ktnes.Memory
import com.terorie.ktnes.ROM
import com.terorie.ktnes.UnsignedByteArray

class NROM(
        val prgRom: ExtendedBytes,
        prgRamSize: Int,
        val chrRom: ExtendedBytes
) : ROM {

    init {
        when (prgRom.size) {
            16384, 32768 -> /* absolute */ Unit
            else -> throw IllegalArgumentException("NROM has either 16 KiB or 32 KiB of PRG ROM")
        }
        when (prgRamSize) {
            2048, 4096 -> Unit
            else -> throw IllegalArgumentException("NROM has either 2 KiB or 4 KiB of PRG RAM")
        }
        if (chrRom.size != 8192) {

        }
    }

    val prgRam = UnsignedByteArray(prgRamSize)

    override val name: String
        get() = "NROM ROM"

    override val cpuSpace: Memory get() = object : Memory {
        override fun read(addr: Int): Int = when {
            addr < 0x6000 ->
                0x00
            addr < 0x8000 ->
                prgRam[addr % prgRam.size]
            else ->
                prgRom[addr % prgRom.size]
        }

        override fun write(addr: Int, x: Int) {
            if (addr in 0x6000..0x8000)
                prgRam[addr % prgRam.size] = x
        }
    }



}
