package com.terorie.ktnes

class NES {

    val cpuMemory = CPUMemory(this)
    val cpu = CPU(cpuMemory)

    // $4000 -> $401F
    val ioRegisters = UnsignedByteArray(32)

    val ppu = PPU()
    var rom: ROM? = null

}
