package com.terorie.ktnes

open class NESException(msg: String = "") : Exception(msg)

open class CPUException : NESException()
class IllegalOpcode : CPUException()

class UnmappedMemoryAccess : NESException()

class ROMException(msg: String) : NESException(msg)
