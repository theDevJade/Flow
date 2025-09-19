package com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.misc

/**
 * Thrown by ReturnNode to unwind out of the current function body.
 */
class ReturnException(val value: Any?) : Exception()
