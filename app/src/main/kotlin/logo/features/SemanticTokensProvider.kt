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
        // TODO: Implement token type classification based on TokenType
        // For now, return empty (placeholder)
        return SemanticTokens(emptyList())
    }

    /**
     * Map a token type to its semantic token type index.
     */
    private fun tokenTypeToIndex(tokenType: TokenType): Int {
        return when (tokenType) {
            // Keywords
            TokenType.TO, TokenType.END,
            TokenType.IF, TokenType.IFELSE,
            TokenType.REPEAT, TokenType.WHILE, TokenType.FOR, TokenType.FOREACH, TokenType.FOREVER,
            TokenType.OUTPUT, TokenType.STOP,
            TokenType.MAKE, TokenType.LOCALMAKE, TokenType.LOCAL,
            TokenType.AND, TokenType.OR, TokenType.NOT -> 0 // "keyword"

            // Builtin commands
            TokenType.WORD -> 1 // "function" (will be filtered by semantic analyzer)

            // Parameter (procedure parameter)
            TokenType.VARIABLE_REF -> 3 // "variable"

            // Literals
            TokenType.NUMBER -> 4 // "number"
            TokenType.QUOTED_WORD -> 5 // "string"
            TokenType.BOOLEAN_TRUE, TokenType.BOOLEAN_FALSE -> 4 // "number"

            // Comments
            TokenType.COMMENT -> 6 // "comment"

            // Operators
            TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH,
            TokenType.EQ, TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE, TokenType.NEQ -> 7 // "operator"

            // Structural tokens (not highlighted)
            else -> -1 // Skip
        }
    }
}
