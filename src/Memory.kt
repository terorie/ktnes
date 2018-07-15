package com.terorie.ktnes

interface Memory {

    fun read(addr: Int): Int
    fun write(addr: Int, x: Int)

    fun read16(addr: Int) = readShort(read(addr), read(addr add16 1))

}

class CPUMemory(val n: NES) : Memory {

    private val ram = UnsignedByteArray(2048)

    override fun read(addr: Int): Int = when {
        // 2kb internal RAM
        addr < 0x2000 ->
            ram[addr % 0x0800]

        // PPU registers
        addr < 0x4000 ->
            n.ppu.registers[addr % 0x8]

        // APU and I/O registers
        addr < 0x4020 ->
            n.ioRegisters[addr % 0x32]

        // Cartridge space
        else ->
            n.rom?.cpuSpace?.read(addr) ?: 0x00
    }

    override fun write(addr: Int, x: Int) = when {
        // 2kb internal RAM
        addr < 0x2000 ->
            ram[addr % 0x0800] = x

        // PPU registers
        addr < 0x4000 ->
            n.ppu.registers[addr % 0x8] = x

        // APU and I/O registers
        addr < 0x4020 ->
            n.ioRegisters[addr % 0x32] = x

        // Cartridge space
        else ->
            n.rom?.cpuSpace?.write(addr, x) ?: Unit
    }

}
