package logo.lexer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LexerTest {

    private fun tokenize(source: String): List<Token> = Lexer(source).tokenize()

    private fun tokenTypes(source: String): List<TokenType> =
        tokenize(source).map { it.type }.filter { it != TokenType.EOF }

    @Test
    fun `empty input produces only EOF`() {
        val tokens = tokenize("")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.EOF, tokens[0].type)
    }

    @Test
    fun `integer number`() {
        val tokens = tokenize("100")
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("100", tokens[0].text)
    }

    @Test
    fun `decimal number`() {
        val tokens = tokenize("3.14")
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("3.14", tokens[0].text)
    }

    @Test
    fun `number starting with dot`() {
        val tokens = tokenize(".5")
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals(".5", tokens[0].text)
    }

    @Test
    fun `variable reference`() {
        val tokens = tokenize(":size")
        assertEquals(TokenType.VARIABLE_REF, tokens[0].type)
        assertEquals(":size", tokens[0].text)
    }

    @Test
    fun `variable reference with digits`() {
        val tokens = tokenize(":var1")
        assertEquals(TokenType.VARIABLE_REF, tokens[0].type)
        assertEquals(":var1", tokens[0].text)
    }

    @Test
    fun `quoted word`() {
        val tokens = tokenize("\"hello")
        assertEquals(TokenType.QUOTED_WORD, tokens[0].type)
        assertEquals("\"hello", tokens[0].text)
    }

    @Test
    fun `comment`() {
        val tokens = tokenize("; this is a comment")
        assertEquals(TokenType.COMMENT, tokens[0].type)
        assertEquals("; this is a comment", tokens[0].text)
    }

    @Test
    fun `comment does not consume newline`() {
        val types = tokenTypes("; comment\nFD")
        assertEquals(listOf(TokenType.COMMENT, TokenType.NEWLINE, TokenType.WORD), types)
    }

    @Test
    fun `keywords are case insensitive`() {
        assertEquals(TokenType.TO, tokenize("TO")[0].type)
        assertEquals(TokenType.TO, tokenize("to")[0].type)
        assertEquals(TokenType.TO, tokenize("To")[0].type)
    }

    @Test
    fun `keyword original text preserved`() {
        val token = tokenize("repeat")[0]
        assertEquals(TokenType.REPEAT, token.type)
        assertEquals("repeat", token.text)
    }

    @Test
    fun `all keywords recognized`() {
        val cases = mapOf(
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
        for ((text, expectedType) in cases) {
            assertEquals(expectedType, tokenize(text)[0].type, "Failed for keyword: $text")
        }
    }

    @Test
    fun `bare word not matching keyword`() {
        val token = tokenize("FORWARD")[0]
        assertEquals(TokenType.WORD, token.type)
        assertEquals("FORWARD", token.text)
    }

    @Test
    fun `word with question mark`() {
        val token = tokenize("EMPTYP")[0]
        assertEquals(TokenType.WORD, token.type)

        val token2 = tokenize("empty?")[0]
        assertEquals(TokenType.WORD, token2.type)
        assertEquals("empty?", token2.text)
    }

    @Test
    fun `operators`() {
        assertEquals(TokenType.PLUS, tokenize("+")[0].type)
        assertEquals(TokenType.MINUS, tokenize("-")[0].type)
        assertEquals(TokenType.STAR, tokenize("*")[0].type)
        assertEquals(TokenType.SLASH, tokenize("/")[0].type)
        assertEquals(TokenType.EQ, tokenize("=")[0].type)
        assertEquals(TokenType.LT, tokenize("<")[0].type)
        assertEquals(TokenType.GT, tokenize(">")[0].type)
        assertEquals(TokenType.LTE, tokenize("<=")[0].type)
        assertEquals(TokenType.GTE, tokenize(">=")[0].type)
        assertEquals(TokenType.NEQ, tokenize("!=")[0].type)
        assertEquals(TokenType.NEQ, tokenize("<>")[0].type)
    }

    @Test
    fun `brackets`() {
        assertEquals(TokenType.LBRACKET, tokenize("[")[0].type)
        assertEquals(TokenType.RBRACKET, tokenize("]")[0].type)
        assertEquals(TokenType.LPAREN, tokenize("(")[0].type)
        assertEquals(TokenType.RPAREN, tokenize(")")[0].type)
    }

    @Test
    fun `newlines`() {
        val types = tokenTypes("a\nb")
        assertEquals(listOf(TokenType.WORD, TokenType.NEWLINE, TokenType.WORD), types)
    }

    @Test
    fun `whitespace is skipped`() {
        val types = tokenTypes("FD   100")
        assertEquals(listOf(TokenType.WORD, TokenType.NUMBER), types)
    }

    @Test
    fun `procedure definition`() {
        val types = tokenTypes("TO SQUARE :size\n  REPEAT 4 [FD :size RT 90]\nEND")
        assertEquals(
            listOf(
                TokenType.TO, TokenType.WORD, TokenType.VARIABLE_REF, TokenType.NEWLINE,
                TokenType.REPEAT, TokenType.NUMBER, TokenType.LBRACKET,
                TokenType.WORD, TokenType.VARIABLE_REF, TokenType.WORD, TokenType.NUMBER,
                TokenType.RBRACKET, TokenType.NEWLINE,
                TokenType.END,
            ),
            types
        )
    }

    @Test
    fun `variable assignment`() {
        val types = tokenTypes("MAKE \"counter 0")
        assertEquals(
            listOf(TokenType.MAKE, TokenType.QUOTED_WORD, TokenType.NUMBER),
            types
        )
    }

    @Test
    fun `arithmetic expression`() {
        val types = tokenTypes(":x + :y * 2")
        assertEquals(
            listOf(
                TokenType.VARIABLE_REF, TokenType.PLUS,
                TokenType.VARIABLE_REF, TokenType.STAR, TokenType.NUMBER,
            ),
            types
        )
    }

    @Test
    fun `if statement`() {
        val types = tokenTypes("IF :x > 100 [STOP]")
        assertEquals(
            listOf(
                TokenType.IF, TokenType.VARIABLE_REF, TokenType.GT, TokenType.NUMBER,
                TokenType.LBRACKET, TokenType.STOP, TokenType.RBRACKET,
            ),
            types
        )
    }

    @Test
    fun `line and column tracking`() {
        val tokens = tokenize("FD 100\nRT 90")
        val fd = tokens[0]
        assertEquals(0, fd.line)
        assertEquals(0, fd.column)

        val num100 = tokens[1]
        assertEquals(0, num100.line)
        assertEquals(3, num100.column)

        val newline = tokens[2]
        assertEquals(0, newline.line)

        val rt = tokens[3]
        assertEquals(1, rt.line)
        assertEquals(0, rt.column)

        val num90 = tokens[4]
        assertEquals(1, num90.line)
        assertEquals(3, num90.column)
    }

    @Test
    fun `token length`() {
        val tokens = tokenize("REPEAT 100")
        assertEquals(6, tokens[0].length)
        assertEquals(3, tokens[1].length)
    }

    @Test
    fun `complex program`() {
        val source = """
            TO SPIRAL :size :angle
              IF :size > 100 [STOP]
              FD :size RT :angle
              SPIRAL :size + 2 :angle
            END

            MAKE "count 10
            SPIRAL 5 90
        """.trimIndent()
        val tokens = tokenize(source)
        assertTrue(tokens.size > 10)
        assertEquals(TokenType.TO, tokens[0].type)
        assertEquals(TokenType.EOF, tokens.last().type)
    }

    @Test
    fun `boolean literals`() {
        assertEquals(TokenType.BOOLEAN_TRUE, tokenize("TRUE")[0].type)
        assertEquals(TokenType.BOOLEAN_FALSE, tokenize("false")[0].type)
    }

    @Test
    fun `multiple consecutive newlines`() {
        val types = tokenTypes("FD\n\n\n100")
        assertEquals(
            listOf(TokenType.WORD, TokenType.NEWLINE, TokenType.NEWLINE, TokenType.NEWLINE, TokenType.NUMBER),
            types
        )
    }

    @Test
    fun `carriage return line feed`() {
        val types = tokenTypes("FD\r\n100")
        assertEquals(listOf(TokenType.WORD, TokenType.NEWLINE, TokenType.NUMBER), types)
    }

    @Test
    fun `lone colon`() {
        val token = tokenize(":")[0]
        assertEquals(TokenType.VARIABLE_REF, token.type)
        assertEquals(":", token.text)
    }

    @Test
    fun `quoted word stops at delimiter`() {
        val tokens = tokenize("\"hello :world")
        assertEquals(TokenType.QUOTED_WORD, tokens[0].type)
        assertEquals("\"hello", tokens[0].text)
        assertEquals(TokenType.VARIABLE_REF, tokens[1].type)
    }
}
