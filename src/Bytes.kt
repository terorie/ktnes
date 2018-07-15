package com.terorie.ktnes

import kotlin.math.min

open class UnsignedByteArray(val size: Int) {

    private val arr = ByteArray(size)

    operator fun get(index: Int): Int {
        return arr[index].toInt() and 0xFF
    }

    operator fun set(index: Int, value: Int) {
        arr[index] = value.toByte()
    }

}

class ExtendedBytes(
        val content: UnsignedByteArray,
        val writable: Boolean,
        val size: Int,
        val fill: Int
) {

    operator fun get(index: Int): Int = when {
        index < content.size -> content[index]
        index < size -> fill
        else -> throw IndexOutOfBoundsException()
    }

    operator fun set(index: Int, value: Int) {
        if (writable) return when {
            index < content.size -> content[index] = value
            index < size -> Unit
            else -> throw IndexOutOfBoundsException()
        }
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

}

fun compareByteArrays(
        a1: ByteArray, a2: ByteArray,
        length: Int = min(a1.size, a2.size),
        start1: Int = 0, start2: Int = 0
): Boolean {
    for (i in 0 until length) {
        if (a1[start1 + i] != a2[start2 + i])
            return false
    }
    return true
}
