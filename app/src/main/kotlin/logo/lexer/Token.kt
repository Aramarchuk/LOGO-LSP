package logo.lexer

enum class TokenType {
    // Literals
    NUMBER,
    BOOLEAN_TRUE,
    BOOLEAN_FALSE,

    // Identifiers & references
    WORD,
    VARIABLE_REF,   // :varname
    QUOTED_WORD,     // "word

    // Structure
    TO,
    END,

    // Control
    IF,
    IFELSE,
    REPEAT,
    WHILE,
    FOR,
    FOREACH,
    FOREVER,

    // Flow
    OUTPUT,
    STOP,

    // Variables
    MAKE,
    LOCALMAKE,
    LOCAL,

    // Logical
    AND,
    OR,
    NOT,

    // Brackets
    LBRACKET,
    RBRACKET,
    LPAREN,
    RPAREN,

    // Operators
    PLUS,
    MINUS,
    STAR,
    SLASH,
    EQ,
    LT,
    GT,
    LTE,
    GTE,
    NEQ,

    // Special
    COMMENT,
    NEWLINE,
    EOF,
}

data class Token(
    val type: TokenType,
    val text: String,
    val line: Int,
    val column: Int,
    val length: Int = text.length,
)
