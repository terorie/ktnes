@file:Suppress("NOTHING_TO_INLINE")
package com.terorie.ktnes

import kotlin.reflect.KProperty

// MOS 6502, no BCD

class CPU(val mem: Memory) {

    companion object {
        // Address modes
        const val AM_IMM      = -2 // #immediate
        const val AM_ACCU     = -1 // accumulator
        const val AM_ZP       =  1 // zero page
        const val AM_ZP_X     =  2 // zero page, x
        const val AM_ZP_Y     =  3 // zero page, y
        const val AM_ZP_X_PTR =  4 // (zero page, x)
        const val AM_ZP_PTR_Y =  5 // (zero page), y
        const val AM_ABS      =  6 // absolute
        const val AM_ABS_X    =  7 // absolute, x
        const val AM_ABS_Y    =  8 // absolute, y
    }

    // Cycle count
    // var cycle = 0L

    // General purpose register
    var rA = 0
    var rX = 0
    var rY = 0

    // Stack pointer register
    var rS = 0

    // Program counter register
    var rPC = 0

    // Status flags register
    var rP = 0b0000_0100 // TODO bit 5?

    // Status flags
    var sCarry    by StatusDelegate(0)
    var sZero     by StatusDelegate(1)
    var sID       by StatusDelegate(2)
    var sDecimal  by StatusDelegate(3)
    var sOverflow by StatusDelegate(6)
    var sNegative by StatusDelegate(7)

    // Moves the program counter by one and reads
    fun readProgram(): Int {
        val x = mem.read(rPC)
        rPC = rPC.inc16()
        // TODO Handle overflow
        return x
    }

    // Gets the address from a location
    private fun locToAddr(am: Int): Int = when(am) {
        AM_ZP    -> readProgram()
        AM_ZP_X  -> readProgram() + rX
        AM_ZP_Y  -> readProgram() + rY
        AM_ABS   -> readShort(readProgram(), readProgram())
        AM_ABS_X -> readShort(readProgram(), readProgram()) + rX
        AM_ABS_Y -> readShort(readProgram(), readProgram()) + rY
        else -> throw IllegalArgumentException()
    }

    // Reads a byte from the memory or a register
    fun readLocation(am: Int): Int = when(am) {
        AM_ACCU -> rA
        AM_IMM  -> readProgram()
        else    -> locToAddr(am)
    }

    // Writes a byte to the memory or a register
    fun writeLocation(am: Int, x: Int) = when(am) {
        AM_ACCU -> rA
        AM_IMM  -> readProgram()
        else    -> locToAddr(am)
    }

    // Updates a byte in memory or in a register
    inline fun updateLocation(am: Int, f: (Int) -> Int) {
        val old = readLocation(am)
        val new = f(old)
        writeLocation(am, new)
    }

    // Get address under stack pointer
    inline fun stackAddress() = 0x0100 + rS

    // Push a byte to stack
    fun pushStack(x: Int) {
        mem.write(stackAddress(), x)
        rS.dec8()
    }

    // Pull a byte from stack
    fun pullStack(): Int {
        val x = mem.read(stackAddress())
        rS.inc8()
        return x
    }

    // Reads, decodes and executes an instruction
    fun next() {
        // Gets the opcode
        val op = readProgram()

        // Instruction group
        val ig = op and 0b0000_0011

        // Instruction index
        val ii = (op and 0b1110_0000) shr 5

        // Addressing mode
        val am = (op and 0b0001_1100) shr 2

        when (ig) {
            0b00 -> next00(ii, am)
            0b01 -> next01(ii, am)
            0b10 -> next10(ii, am)
            else -> throw IllegalOpcode()
        }
    }

    // Next group 00 instruction
    private inline fun next00(ii: Int, am: Int) {
        when {
            am == 0 && ii < 4 ->
                next00_am000(ii)
            am == 0b010 ->
                next00_am010(ii)
            am == 0b100 ->
                next00_am100(ii)
            am == 0b110 ->
                next00_am110(ii)
            else ->
                next00_standard(ii, am)
        }
    }

    // Next group 00 instruction (index < 4) with addressing mode 000
    private inline fun next00_am000(ii: Int) = when(ii) {
        0 -> opBRK()
        1 -> opJSRabs()
        2 -> opRTI()
        3 -> opRTS()
        else -> throw IllegalArgumentException()
    }

    // Next group 00 instruction with addressing mode 010
    private inline fun next00_am010(ii: Int) = when(ii) {
        0 -> opPHP()
        1 -> opPLP()
        2 -> opPHA()
        3 -> opPLA()
        4 -> opDEY()
        5 -> opTAY()
        6 -> opINY()
        7 -> opINX()
        else -> throw IllegalArgumentException()
    }

    // Next group 00 instruction with addressing mode 110
    private inline fun next00_am110(ii: Int) = when(ii) {
        0 -> opCLC()
        1 -> opSEC()
        2 -> opCLI()
        3 -> opSEI()
        4 -> opTYA()
        5 -> opCLV()
        6 -> opCLD()
        7 -> opSED()
        else -> throw IllegalArgumentException()
    }

    // Next standard group 00 instruction
    private inline fun next00_standard(ii: Int, am: Int) = when(ii) {
        0 -> throw IllegalOpcode()
        1 -> opBIT(am)
        2 -> opJMP(am)
        3 -> opJMPabs(am)
        4 -> opSTY(am)
        5 -> opLDY(am)
        6 -> opCPY(am)
        7 -> opCPX(am)
        else -> throw IllegalOpcode()
    }

    // Decodes the addressing mode for instruction group 00
    private fun am00(am: Int): Int = when(am) {
        0 -> AM_IMM
        1 -> AM_ZP
        3 -> AM_ABS
        5 -> AM_ZP_X
        7 -> AM_ABS_X
        2, 4, 6 -> throw IllegalOpcode()
        else -> throw IllegalArgumentException()
    }

    // Next group 00 instruction with addressing mode 100 (branches)
    private inline fun next00_am100(ii: Int) {
        // Branch type
        val bt = (ii and 0b110) shr 1
        // Branch condition
        val bc = (ii and 1) == 1
        // Branch target
        val offset = readShort(readProgram(), readProgram())

        val doBranch = when (bt) {
            0 -> bc == sNegative
            1 -> bc == sOverflow
            2 -> bc == sCarry
            3 -> bc == sZero
            else -> throw IllegalStateException()
        }

        if (doBranch)
            rPC += offset
    }

    // Next group 01 instruction
    private inline fun next01(ii: Int, am: Int) = when (ii) {
        0 -> opORA(am)
        1 -> opAND(am)
        2 -> opEOR(am)
        3 -> opADC(am)
        4 -> opSTA(am)
        5 -> opLDA(am)
        6 -> opCMP(am)
        7 -> opSBC(am)
        else -> throw IllegalArgumentException()
    }

    // Decodes the addressing mode for instruction group 01
    private fun am01(am: Int): Int = when(am) {
        0 -> AM_ZP_X_PTR
        1 -> AM_ZP
        2 -> AM_IMM
        3 -> AM_ABS
        4 -> AM_ZP_PTR_Y
        5 -> AM_ZP_X
        6 -> AM_ABS_Y
        7 -> AM_ABS_X
        else -> throw IllegalArgumentException()
    }

    // Next group 10 instruction
    private fun next10(ii: Int, am: Int) {
        if (ii >= 4) {
            when (am) {
                0b010 -> next10_am010(ii)
                0b110 -> next10_am110(ii)
            }
        } else {
            next10_standard(ii, am)
        }
    }

    // Next group 10 instruction (index >= 4) with addressing mode 010
    private fun next10_am010(ii: Int) = when (ii) {
        4 -> opTXA()
        5 -> opTAX()
        6 -> opDEX()
        7 -> opNOP()
        else -> throw IllegalArgumentException()
    }

    // Next group 00 instruction with addressing mode 110
    private fun next10_am110(ii: Int) = when (ii) {
        4 -> opTXS()
        5 -> opTSX()
        6, 7 -> throw IllegalOpcode()
        else -> throw IllegalArgumentException()
    }

    // Next standard group 10 instruction
    private fun next10_standard(ii: Int, amb: Int) = when (ii) {
        0 -> opASL(amb)
        1 -> opROL(amb)
        2 -> opLSR(amb)
        3 -> opROR(amb)
        4 -> opSTX(amb)
        5 -> opLDX(amb)
        6 -> opDEC(amb)
        7 -> opINC(amb)
        else -> throw IllegalArgumentException()
    }

    // Decodes the addressing mode for instruction group 10
    private fun am10(amb: Int): Int = when(amb) {
        0 -> AM_IMM
        1 -> AM_ZP
        2 -> AM_ACCU
        3 -> AM_ABS
        5 -> AM_ZP_X
        7 -> AM_ABS_X
        4, 6 -> throw IllegalOpcode()
        else -> throw IllegalArgumentException()
    }

    // Bitwise OR with Accumulator
    private inline fun opORA(amb: Int) {
        val x = readLocation(am01(amb))
        rA = rA or x
        sZero = rA == 0
        sNegative = rA.bit(7)
    }

    // Bitwise AND with Accumulator
    private inline fun opAND(amb: Int) {
        val x = readLocation(am01(amb))
        rA = rA and x
        sZero = rA == 0
        sNegative = rA.bit(7)
    }

    // Bitwise XOR with Accumulator
    private inline fun opEOR(amb: Int) {
        val x = readLocation(am01(amb))
        rA = rA xor x
        sZero = rA == 0
        sNegative = rA.bit(7)
    }

    // Add with carry
    private inline fun opADC(amb: Int) {
        // TODO
    }

    // Store Accumulator
    private inline fun opSTA(amb: Int) {
        val am = am01(amb)
        if (am == AM_IMM)
            throw IllegalOpcode()

        writeLocation(am, rA)
    }

    // Load Accumulator
    private inline fun opLDA(amb: Int) {
        rA = readLocation(am01(amb))
    }

    // Compare
    private inline fun opCMP(amb: Int) {
        val x = readLocation(am01(amb))
        sCarry = rA >= x
        sZero = rA == x
        sNegative = rA < x
    }

    // Subtract with Carry
    private inline fun opSBC(amb: Int) {
        val am = am01(amb)
        val x = readLocation(am)
        // TODO
    }

    // Arithmetic Shift Left
    private inline fun opASL(amb: Int) {
        // Blacklist #immediate mode
        if (amb == 0b000)
            throw IllegalOpcode()

        updateLocation(am10(amb)) { x ->
            sCarry = x.bit(7)
            val new = (x shl 1) and 0xFF
            sZero = new == 0
            sNegative = new.bit(7)
            new
        }
    }

    // Rotate Left
    private inline fun opROL(amb: Int) {
        val am = am10(amb)
        if (am == AM_IMM)
            throw IllegalOpcode()

        updateLocation(am) { x ->
            val bit0 = sCarry.toInt()
            sCarry = x.bit(7)
            val new = ((x shl 1) or bit0) and 0xFF
            sZero = new == 0
            sNegative = new.bit(7)
            new
        }
    }

    // Logical Shift Right
    private inline fun opLSR(amb: Int) {
        val am = am10(amb)
        if (am == AM_IMM)
            throw IllegalOpcode()

        updateLocation(am) { x ->
            sCarry = x.bit(0)
            val new = x shr 1
            sZero = x == 0
            sNegative = new.bit(7)
            new
        }
    }

    // Rotate Right
    private inline fun opROR(amb: Int) {
        val am = am10(amb)
        if (am == AM_IMM)
            throw IllegalOpcode()

        updateLocation(am) { x ->
            val bit7 = sCarry.toInt()
            sCarry = x.bit(0)
            (x shr 1) or bit7
        }
    }

    // Store X Register
    private inline fun opSTX(amb: Int) {
        var am = am01(amb)

        when(am) {
            AM_ZP_X -> am = AM_ZP_Y
            AM_ACCU, AM_ABS_X -> throw IllegalOpcode()
        }

        writeLocation(am, rX)
    }

    // Load X Register
    private inline fun opLDX(amb: Int) {
        val am = am10(amb)
        if (am == AM_ACCU)
            throw IllegalOpcode()

        rX = readLocation(am)
        sZero = rX == 0
        sNegative = rX.bit(7)
    }

    // Decrement Memory
    private inline fun opDEC(amb: Int) {
        val am = am01(amb)
        when (am) {
            AM_IMM, AM_ACCU -> throw IllegalOpcode()
        }

        updateLocation(am) { x -> x.dec8() }
    }

    // Increment Memory
    private inline fun opINC(amb: Int) {
        val am = am01(amb)
        when (am) {
            AM_IMM, AM_ACCU -> throw IllegalOpcode()
        }

        updateLocation(am) { x -> x.inc8() }
    }

    // Bit Test
    private inline fun opBIT(amb: Int) {
        val am = am00(amb)
        if (am != AM_ZP && am != AM_ABS)
            throw IllegalOpcode()

        val x = readLocation(am)

        val res = rA and x
        sZero = res == 0
        sOverflow = x.bit(6)
        sNegative = x.bit(7)
    }

    // Jump
    private inline fun opJMP(amb: Int) {
        val am = am00(amb)
        if (am != AM_ABS)
            throw IllegalOpcode()
        val addr = readShort(readProgram(), readProgram())
        rPC = addr
    }

    // Jump (absolute)
    private inline fun opJMPabs(amb: Int) {
        val am = am00(amb)
        if (am != AM_ABS)
            throw IllegalOpcode()

        val addr = readShort(readProgram(), readProgram())

        // Indirect JMP bug
        val addr2 = if (addr % 0x100 == 0xFF)
            readShort(mem.read(addr), mem.read(addr) - 0xFF)
        else
            mem.read16(addr)

        rPC = addr2
    }

    // Store Y Register
    private inline fun opSTY(amb: Int) {
        val am = am10(amb)

        when(am) {
            AM_IMM, AM_ABS_X -> throw IllegalOpcode()
        }

        writeLocation(am, rY)
    }

    // Load Y Register
    private inline fun opLDY(am: Int) {
        rY = readLocation(am10(am))
    }

    // Compare Y Register
    private inline fun opCPY(amb: Int) {
        val am = am10(amb)

        when (am) {
            AM_ZP_X, AM_ABS_X -> throw IllegalOpcode()
        }

        val x = readLocation(am)
        sCarry = rY >= x
        sZero  = rY == x
        sNegative = rY < x
    }

    // Compare X Register
    private inline fun opCPX(amb: Int) {
        val am = am10(amb)

        when (am) {
            AM_ZP_X, AM_ABS_X -> throw IllegalOpcode()
        }

        val x = readLocation(am)
        sCarry = rX >= x
        sZero  = rX == x
        sNegative = rX < x
    }

    // Break
    private inline fun opBRK() {
        pushStack(rPC add16 2)
        pushStack(rP)
        val interruptAddr = mem.read16(0xFFFE)
        rPC = interruptAddr
    }

    // Jump to Subroutine
    private inline fun opJSRabs() {
        val addr = readShort(readProgram(), readProgram())
        val returnAddress = rPC sub16 1
        pushStack(returnAddress shr 8)
        pushStack(returnAddress and 0xFF)
        rPC = addr
    }

    // Return from Interrupt
    private inline fun opRTI() {
        rP = pullStack()
        rPC = readShort(pullStack(), pullStack())
    }

    // Return from Subroutine
    private inline fun opRTS() {
        rPC = readShort(pullStack(), pullStack()) add16 1
    }

    // Push Processor Status
    private inline fun opPHP() {
        pushStack(rP)
    }

    // Pull Processor Status
    private inline fun opPLP() {
        rP = pullStack()
    }

    // Push Accumulator
    private inline fun opPHA() {
        pushStack(rA)
    }

    // Pull Accumulator
    private inline fun opPLA() {
        rA = pullStack()
    }

    // Decrement Y Register
    private inline fun opDEY() {
        rY = rY.dec8()
        sZero = rY == 0
        sNegative = rY.bit(7)
    }

    // Transfer Accumulator to Y
    private inline fun opTAY() {
        rY = rA
        sZero = rY == 0
        sNegative = rY.bit(7)
    }

    // Increment Y Register
    private inline fun opINY() {
        rY = rY.inc8()
        sZero = rY == 0
        sNegative = rY.bit(7)
    }

    // Increment X Register
    private inline fun opINX() {
        rX = rX.inc8()
        sZero = rX == 0
        sNegative = rX.bit(7)
    }

    // Clear Carry Flag
    private inline fun opCLC() {
        sCarry = false
    }

    // Set Carry Flag
    private inline fun opSEC() {
        sCarry = true
    }

    // Clear Interrupt Disable
    private inline fun opCLI() {
        sID = false
    }

    // Set Interrupt Disable
    private inline fun opSEI() {
        sID = true
    }

    // Transfer Y to Accumulator
    private inline fun opTYA() {
        rA = rY
        sZero = rA == 0
        sNegative = rA.bit(7)
    }

    // Clear Overflow Flag
    private inline fun opCLV() {
        sOverflow = false
    }

    // Clear Decimal Flag
    private inline fun opCLD() {
        sDecimal = false
    }

    // Set Decimal Flag
    private inline fun opSED() {
        sDecimal = true
    }

    // Transfer X to Accumulator
    private inline fun opTXA() {
        rA = rX
        sZero = rA == 0
        sNegative = rA.bit(7)
    }

    // Transfer Accumulator to X
    private inline fun opTAX() {
        rX = rA
        sZero = rX == 0
        sNegative = rX.bit(7)
    }

    // Decrement X Register
    private inline fun opDEX() {
        rX = rX.dec8()
        sZero = rX == 0
        sNegative = rX.bit(7)
    }

    // No Operation
    private inline fun opNOP() {}

    // Transfer X to Stack Pointer
    private inline fun opTXS() {
        rS = rX
    }

    // Transfer Stack Pointer to X
    private inline fun opTSX() {
        rX = rS
        sZero = rX == 0
        sNegative = rX.bit(7)
    }

    // Delegate status flags to status register
    class StatusDelegate(val index: Int) {
        inline operator fun getValue(cpu: CPU, p: KProperty<*>): Boolean {
            return (cpu.rP shr index) == 1
        }
        inline operator fun setValue(cpu: CPU, p: KProperty<*>, value: Boolean) {
            cpu.rP = if (value)
                (cpu.rP or index)
            else
                (cpu.rP and (1 shl index).inv())
        }
    }

}
