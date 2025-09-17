package com.thedevjade.io.flowlang.language.parsing

import java.util.regex.Pattern

/**
 * Flexible pre-processor that converts natural-language phrases into
 * classic FlowLang symbols and removes noise words.
 *
 *  ✦ Phrase rules (Regex → replacement) run first – good for multi‑word tokens.
 *  ✦ Simple token rules (single word → replacement) run next.
 *  ✦ Removable words are then stripped.
 *
 * All three layers are fully extensible at runtime.
 */
class Preprocessor(private val registerDefaults: Boolean = true) {
    private val phraseRules = mutableListOf<Pair<Pattern, String>>()
    private val tokenReplacements = mutableMapOf<String, String>()
    private val removables = mutableSetOf<String>()

    init {
        if (registerDefaults) {
            registerDefaults()
        }
    }

    private fun registerDefaults() {
        // Comparison phrases
        registerPhraseReplacement("\\bis\\s+greater\\s+than\\s+or\\s+equal\\s+to\\b", ">=")
        registerPhraseReplacement("\\bis\\s+less\\s+than\\s+or\\s+equal\\s+to\\b", "<=")
        registerPhraseReplacement("\\bis\\s+greater\\s+than\\b", ">")
        registerPhraseReplacement("\\bis\\s+less\\s+than\\b", "<")
        registerPhraseReplacement("\\bis\\s+not\\s+equal\\s+to\\b", "!=")
        registerPhraseReplacement("\\bis\\s+equal\\s+to\\b|\\bequals\\b", "==")

        // Arithmetic phrases
        registerPhraseReplacement("\\bplus\\b", "+")
        registerPhraseReplacement("\\bminus\\b", "-")
        registerPhraseReplacement("\\btimes\\b|\\bmultiplied\\s+by\\b", "*")
        registerPhraseReplacement("\\bdivided\\s+by\\b", "/")
        registerPhraseReplacement("\\bmodulo\\b|\\bmod\\b", "%")

        // Variable assignment
        registerPhraseReplacement("\\bset\\s+([A-Za-z_]\\w*)\\s+to\\b", "var $1 =")

        // Control structures - only add parentheses if they're missing
        registerPhraseReplacement("\\bif\\s+(?!\\()([^\\n\\s]+(?:\\s+[^\\n\\s]+)*?)\\s*(?=\\s*\\n|$)", "if ($1) {")
        registerPhraseReplacement("\\bwhile\\s+(?!\\()([^\\n\\s]+(?:\\s+[^\\n\\s]+)*?)(?=\\s+then\\b|\\s*\\n|$)", "while ($1)")
        
        // Control structure endings
        registerPhraseReplacement("\\bend\\s+if\\b", "}")
        registerPhraseReplacement("\\bend\\s+while\\b", "")  // Remove entirely for while loops
        registerPhraseReplacement("\\bend\\s+for\\b", "}")
        registerPhraseReplacement("\\bend\\s+function\\b", "}")
        
        // Noise words
        registerRemovableKeyword("then")
    }

    fun registerPhraseReplacement(pattern: String, replacement: String, options: Int = Pattern.CASE_INSENSITIVE) {
        phraseRules.add(Pair(Pattern.compile(pattern, options), replacement))
    }

    fun registerTokenReplacement(word: String, replacement: String) {
        tokenReplacements[word.lowercase()] = replacement
    }

    fun registerRemovableKeyword(keyword: String) {
        removables.add(keyword.lowercase())
    }

    fun process(input: String): String {
        val stringRegex = Pattern.compile("(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')")
        val matcher = stringRegex.matcher(input)
        val result = StringBuilder()
        var lastEnd = 0

        while (matcher.find()) {
            // Add processed text before the string literal
            val beforeString = input.substring(lastEnd, matcher.start())
            result.append(processText(beforeString))
            
            // Add the string literal unchanged
            result.append(matcher.group())
            
            lastEnd = matcher.end()
        }
        
        // Add any remaining text
        if (lastEnd < input.length) {
            result.append(processText(input.substring(lastEnd)))
        }
        
        return result.toString()
    }
    
    private fun processText(text: String): String {
        val lines = text.split('\n').toMutableList()
        for (j in lines.indices) {
            val line = lines[j]
            val trimmed = line.trimStart()

            if (trimmed.startsWith("#")) continue // Skip comments

            lines[j] = processNonStringLine(line)
        }
        return lines.joinToString("\n")
    }

    private fun processNonStringLine(line: String): String {
        var processedLine = line

        // Apply phrase rules
        for ((pattern, replacement) in phraseRules) {
            processedLine = pattern.matcher(processedLine).replaceAll(replacement)
        }

        // Tokenize and process
        val tokenRegex = Pattern.compile("(\\s+|\\w+|\\S)")
        val matcher = tokenRegex.matcher(processedLine)
        val tokens = mutableListOf<String>()

        while (matcher.find()) {
            tokens.add(matcher.group())
        }

        val processed = mutableListOf<String>()
        for (token in tokens) {
            if (removables.contains(token.lowercase())) continue

            val replacement = tokenReplacements[token.lowercase()]
            processed.add(replacement ?: token)
        }

        return mergeWhitespace(processed)
    }

    private fun mergeWhitespace(tokens: List<String>): String {
        val merged = mutableListOf<String>()
        var lastWasSpace = false

        for (token in tokens) {
            val isSpace = token.all { it.isWhitespace() }
            if (isSpace) {
                if (!lastWasSpace) {
                    merged.add(" ")
                }
                lastWasSpace = true
            } else {
                merged.add(token)
                lastWasSpace = false
            }
        }

        return merged.joinToString("")
    }
}
