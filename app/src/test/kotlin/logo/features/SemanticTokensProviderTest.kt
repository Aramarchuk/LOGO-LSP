package logo.features

import logo.lexer.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SemanticTokensProviderTest {

    /**
     * Helper: returns list of triples (tokenText, semanticType, length)
     * by decoding the delta-encoded data back into absolute positions
     * and matching against the original source.
     */
    private fun highlight(source: String): List<Triple<String, String, Int>> {
        val tokens = Lexer(source).tokenize()
        val result = SemanticTokensProvider.computeTokens(tokens)
        val data = result.data
        val legend = SemanticTokensProvider.tokenLegend().tokenTypes

        val highlights = mutableListOf<Triple<String, String, Int>>()
        var line = 0
        var col = 0
        var i = 0
        while (i + 4 < data.size) {
            val deltaLine = data[i]
            val deltaCol = data[i + 1]
            val length = data[i + 2]
            val typeIndex = data[i + 3]
            // data[i + 4] is modifiers, always 0

            line += deltaLine
            col = if (deltaLine > 0) deltaCol else col + deltaCol

            val lines = source.lines()
            val text = if (line < lines.size && col + length <= lines[line].length) {
                lines[line].substring(col, col + length)
            } else {
                "???"
            }

            highlights += Triple(text, legend[typeIndex], length)
            i += 5
        }
        return highlights
    }

    @Test
    fun `keyword is highlighted`() {
        val h = highlight("REPEAT 4 [FD 100]")
        assertTrue(h.any { it.first == "REPEAT" && it.second == "keyword" })
    }

    @Test
    fun `number is highlighted`() {
        val h = highlight("FD 100")
        assertTrue(h.any { it.first == "100" && it.second == "number" })
    }

    @Test
    fun `variable ref is highlighted`() {
        val h = highlight("FD :size")
        assertTrue(h.any { it.first == ":size" && it.second == "variable" })
    }

    @Test
    fun `quoted word is highlighted as string`() {
        val h = highlight("PRINT \"hello")
        assertTrue(h.any { it.first == "\"hello" && it.second == "string" })
    }

    @Test
    fun `comment is highlighted`() {
        val h = highlight("FD 100 ; go forward")
        assertTrue(h.any { it.second == "comment" })
    }

    @Test
    fun `operator is highlighted`() {
        val h = highlight("PRINT 1 + 2")
        assertTrue(h.any { it.first == "+" && it.second == "operator" })
    }

    @Test
    fun `word is highlighted as function`() {
        val h = highlight("FORWARD 100")
        assertTrue(h.any { it.first == "FORWARD" && it.second == "function" })
    }

    @Test
    fun `boolean is highlighted as number`() {
        val h = highlight("MAKE \"flag TRUE")
        assertTrue(h.any { it.first == "TRUE" && it.second == "number" })
    }

    @Test
    fun `multiline delta encoding`() {
        val h = highlight("FD 100\nRT 90")
        // Both lines should have highlights
        assertTrue(h.any { it.first == "FD" })
        assertTrue(h.any { it.first == "RT" })
    }

    @Test
    fun `structural tokens not highlighted`() {
        val h = highlight("[1 2 3]")
        // Brackets should not appear in highlights
        assertFalse(h.any { it.first == "[" })
        assertFalse(h.any { it.first == "]" })
    }

    @Test
    fun `empty source returns empty tokens`() {
        val h = highlight("")
        assertTrue(h.isEmpty())
    }

    @Test
    fun `length matches token length`() {
        val h = highlight("FORWARD 100")
        val fw = h.first { it.first == "FORWARD" }
        assertEquals(7, fw.third)
        val num = h.first { it.first == "100" }
        assertEquals(3, num.third)
    }
}
