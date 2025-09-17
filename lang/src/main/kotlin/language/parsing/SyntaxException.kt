package com.thedevjade.io.flowlang.language.parsing

class SyntaxException(message: String, val token: Token) : Exception(message)
