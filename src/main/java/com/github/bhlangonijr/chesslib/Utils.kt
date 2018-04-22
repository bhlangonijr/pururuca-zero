package com.github.bhlangonijr.chesslib

fun mergeTokens(tokens: List<String>, startToken: String, separator: String): String {

    val start = tokens.indexOf(startToken)
    return mergeTokens(tokens, start + 1, tokens.size - 1, separator)
}

fun mergeTokens(tokens: List<String>, startToken: String, endToken: String, separator: String): String {

    val start = tokens.indexOf(startToken)
    val end = tokens.indexOf(endToken) - 1
    return mergeTokens(tokens, start + 1, end, separator)
}

fun mergeTokens(tokens: List<String>, start: Int, end: Int, separator: String): String {

    val str = StringBuilder()
    for (i in start..end) {
        str.append("${tokens[i]}$separator")
    }
    return str.toString()
}

fun getArg(tokens: List<String>, startToken: String): String? {

    val start = tokens.indexOf(startToken)
    if (start == -1) {
        return null
    }
    return mergeTokens(tokens, start + 1, start + 1, "")
}
