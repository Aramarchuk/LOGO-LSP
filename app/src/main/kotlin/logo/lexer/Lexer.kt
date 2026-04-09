package logo.lexer

import java.util.Locale

class Lexer(private val source: String) {

    private var pos = 0
    private var line = 0
    private var column = 0

    companion object {
        private val keywords = mapOf(
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
    }

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < source.length) {
            val ch = source[pos]
            when {
                ch == '\n' -> {
                    tokens.add(Token(TokenType.NEWLINE, "\n", line, column))
                    advance()
                    line++
                    column = 0
                }
                ch == '\r' -> {
                    val startCol = column
                    advance()
                    if (pos < source.length && source[pos] == '\n') advance()
                    tokens.add(Token(TokenType.NEWLINE, "\n", line, startCol))
                    line++
                    column = 0
                }
                ch == ' ' || ch == '\t' -> advance()
                ch == ';' -> tokens.add(readComment())
                ch == ':' -> tokens.add(readVariableRef())
                ch == '"' -> tokens.add(readQuotedWord())
                ch == '[' -> { tokens.add(Token(TokenType.LBRACKET, "[", line, column)); advance() }
                ch == ']' -> { tokens.add(Token(TokenType.RBRACKET, "]", line, column)); advance() }
                ch == '(' -> { tokens.add(Token(TokenType.LPAREN, "(", line, column)); advance() }
                ch == ')' -> { tokens.add(Token(TokenType.RPAREN, ")", line, column)); advance() }
                ch == '+' -> { tokens.add(Token(TokenType.PLUS, "+", line, column)); advance() }
                ch == '*' -> { tokens.add(Token(TokenType.STAR, "*", line, column)); advance() }
                ch == '/' -> { tokens.add(Token(TokenType.SLASH, "/", line, column)); advance() }
                ch == '=' -> { tokens.add(Token(TokenType.EQ, "=", line, column)); advance() }
                ch == '-' -> { tokens.add(Token(TokenType.MINUS, "-", line, column)); advance() }
                ch == '<' -> tokens.add(readLessOrLessEq())
                ch == '>' -> tokens.add(readGreaterOrGreaterEq())
                ch == '!' -> tokens.add(readBangEq())
                ch.isDigit() || (ch == '.' && pos + 1 < source.length && source[pos + 1].isDigit()) -> tokens.add(readNumber())
                ch.isWordChar() -> tokens.add(readWord())
                else -> {
                    tokens.add(Token(TokenType.WORD, ch.toString(), line, column))
                    advance()
                }
            }
        }
        tokens.add(Token(TokenType.EOF, "", line, column))
        return tokens
    }

    private fun readComment(): Token {
        val startCol = column
        val start = pos
        while (pos < source.length && source[pos] != '\n' && source[pos] != '\r') {
            advance()
        }
        return Token(TokenType.COMMENT, source.substring(start, pos), line, startCol)
    }

    private fun readVariableRef(): Token {
        val startCol = column
        val start = pos
        advance() // skip ':'
        while (pos < source.length && source[pos].isWordChar()) {
            advance()
        }
        return Token(TokenType.VARIABLE_REF, source.substring(start, pos), line, startCol)
    }

    private fun readQuotedWord(): Token {
        val startCol = column
        val start = pos
        advance() // skip '"'
        while (pos < source.length && !source[pos].isDelimiter()) {
            advance()
        }
        return Token(TokenType.QUOTED_WORD, source.substring(start, pos), line, startCol)
    }

    private fun readNumber(): Token {
        val startCol = column
        val start = pos
        while (pos < source.length && source[pos].isDigit()) {
            advance()
        }
        if (pos < source.length && source[pos] == '.' && pos + 1 < source.length && source[pos + 1].isDigit()) {
            advance() // skip '.'
            while (pos < source.length && source[pos].isDigit()) {
                advance()
            }
        }
        return Token(TokenType.NUMBER, source.substring(start, pos), line, startCol)
    }

    private fun readWord(): Token {
        val startCol = column
        val start = pos
        while (pos < source.length && source[pos].isWordChar()) {
            advance()
        }
        val text = source.substring(start, pos)
        val type = keywords[text.uppercase(Locale.ROOT)] ?: TokenType.WORD
        return Token(type, text, line, startCol)
    }

    private fun readLessOrLessEq(): Token {
        val startCol = column
        advance() // skip '<'
        if (pos < source.length && source[pos] == '=') {
            advance()
            return Token(TokenType.LTE, "<=", line, startCol)
        }
        if (pos < source.length && source[pos] == '>') {
            advance()
            return Token(TokenType.NEQ, "<>", line, startCol)
        }
        return Token(TokenType.LT, "<", line, startCol)
    }

    private fun readGreaterOrGreaterEq(): Token {
        val startCol = column
        advance()
        if (pos < source.length && source[pos] == '=') {
            advance()
            return Token(TokenType.GTE, ">=", line, startCol)
        }
        return Token(TokenType.GT, ">", line, startCol)
    }

    private fun readBangEq(): Token {
        val startCol = column
        advance() // skip '!'
        if (pos < source.length && source[pos] == '=') {
            advance()
            return Token(TokenType.NEQ, "!=", line, startCol)
        }
        return Token(TokenType.WORD, "!", line, startCol)
    }

    private fun advance() {
        pos++
        column++
    }

    private fun Char.isWordChar(): Boolean =
        isLetterOrDigit() || this == '_' || this == '.' || this == '?'

    private fun Char.isDelimiter(): Boolean = this in " \t\n\r[]();+-*/=<>:"
}
