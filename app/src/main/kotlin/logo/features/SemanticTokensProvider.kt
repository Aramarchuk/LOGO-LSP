package logo.features

import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensLegend
import logo.lexer.Token
import logo.lexer.TokenType

object SemanticTokensProvider {

    /**
     * Token types for semantic highlighting.
     * Order matters: LSP requires these to match the legend.
     */
    private val tokenTypes = listOf(
        "keyword",
        "function",
        "parameter",
        "variable",
        "number",
        "string",
        "comment",
        "operator",
    )

    private val tokenModifiers = listOf<String>()

    /**
     * Return the token legend for semantic tokens.
     * Declares which token types and modifiers are supported.
     */
    fun tokenLegend(): SemanticTokensLegend {
        return SemanticTokensLegend(tokenTypes, tokenModifiers)
    }

    /**
     * Compute semantic tokens for syntax highlighting.
     * Converts lexer tokens to LSP SemanticTokens (delta-encoded 5-int format).
     *
     * LSP uses delta encoding:
     * [deltaLine, deltaStartChar, length, tokenType, tokenModifiers]
     * where deltaLine and deltaStartChar are relative to the previous token.
     */
    fun computeTokens(tokens: List<Token>): SemanticTokens {
        val data = mutableListOf<Int>()
        var prevLine = 0
        var prevCol = 0
        for (token in tokens) {
            val typeIndex = tokenTypeToIndex(token.type)
            if (typeIndex == -1) continue

            val deltaLine = token.line - prevLine
            val deltaCol = if (deltaLine == 0) token.column - prevCol else token.column

            data += deltaLine
            data += deltaCol
            data += token.length
            data += typeIndex
            data += 0

            prevLine = token.line
            prevCol = token.column
        }
        return SemanticTokens(data)
    }

    /**
     * Map a token type to its semantic token type index.
     */
    private fun tokenTypeToIndex(tokenType: TokenType): Int {
        val name = when (tokenType) {
            TokenType.TO, TokenType.END,
            TokenType.IF, TokenType.IFELSE,
            TokenType.REPEAT, TokenType.WHILE, TokenType.FOR, TokenType.FOREACH, TokenType.FOREVER,
            TokenType.OUTPUT, TokenType.STOP,
            TokenType.MAKE, TokenType.LOCALMAKE, TokenType.LOCAL,
            TokenType.AND, TokenType.OR, TokenType.NOT -> "keyword"

            TokenType.WORD -> "function"
            TokenType.VARIABLE_REF -> "variable"
            TokenType.NUMBER, TokenType.BOOLEAN_TRUE, TokenType.BOOLEAN_FALSE -> "number"
            TokenType.QUOTED_WORD -> "string"
            TokenType.COMMENT -> "comment"

            TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.MODULO,
            TokenType.EQ, TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE, TokenType.NEQ -> "operator"

            else -> return -1
        }
        return tokenTypes.indexOf(name)
    }
}
