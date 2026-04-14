package logo.lexer

object LogoKeywords {
    val MapOfKeywords = mapOf(
        "TO" to TokenType.TO,
        "END" to TokenType.END,
        "IF" to TokenType.IF,
        "IFELSE" to TokenType.IFELSE,
        "REPEAT" to TokenType.REPEAT,
        "WHILE" to TokenType.WHILE,
        "FOR" to TokenType.FOR,
        "FOREACH" to TokenType.FOREACH,
        "FOREVER" to TokenType.FOREVER,
        "OUTPUT" to TokenType.OUTPUT,
        "OP" to TokenType.OUTPUT,
        "STOP" to TokenType.STOP,
        "MAKE" to TokenType.MAKE,
        "LOCALMAKE" to TokenType.LOCALMAKE,
        "LOCAL" to TokenType.LOCAL,
        "AND" to TokenType.AND,
        "OR" to TokenType.OR,
        "NOT" to TokenType.NOT,
        "TRUE" to TokenType.BOOLEAN_TRUE,
        "FALSE" to TokenType.BOOLEAN_FALSE,
    )
    val Keywords = MapOfKeywords.keys.sorted()

}