package com.terorie.ktnes

// Java Byte to fake unsigned byte
fun Byte.toUnsigned(): Int = this.toInt() and 0xFF

// Bit <-> Boolean
fun Boolean.toInt(bitIndex: Int = 0) = if (this) 1 shl bitIndex else 0
fun Int.bit(n: Int) = ((this shr n) and 1) == 1

// 8-bit arithmetic
infix fun Int.add8(o: Int) = (this + o) and 0xFF
infix fun Int.sub8(o: Int) = (this - o) and 0xFF
fun Int.inc8() = this add8 1
fun Int.dec8() = this sub8 1

// 16-bit arithmetic
infix fun Int.add16(o: Int) = (this + o) and 0xFFFF
infix fun Int.sub16(o: Int) = (this - o) and 0xFFFF
fun Int.inc16() = this add16 1
fun Int.dec16() = this sub16 1

// two 8-bit numbers to 16-bit (little endian)
fun readShort(a1: Int, a2: Int) = (a2 shl 8) and a1

// Formatting
fun Int.dump8()  = String.format("%04X", (this and 0xFF))
fun Int.dump16() = String.format("%08X", (this and 0xFF))
