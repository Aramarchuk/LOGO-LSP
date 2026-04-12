package logo.features

import logo.lexer.Lexer
import logo.parser.Parser
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GoToDeclarationProviderTest {

    private fun findDecl(source: String, line: Int, col: Int): Pair<Int, Int>? {
        val tokens = Lexer(source).tokenize()
        val result = Parser(tokens).parse()
        val locations = GoToDeclarationProvider.findDeclaration("test.logo", Position(line, col), result)
        val loc = locations.firstOrNull() ?: return null
        return loc.range.start.line to loc.range.start.character
    }

    // -- Variable declarations --

    @Test
    fun `goto procedure parameter`() {
        val source = "TO square :size\n  FD :size\nEND"
        // Click on :size in FD :size (line 1, col 5)
        val target = findDecl(source, 1, 5)
        // Should jump to :size in param list (line 0, col 10)
        assertEquals(0 to 10, target)
    }

    @Test
    fun `goto global make`() {
        val source = "MAKE \"x 5\nPRINT :x"
        // Click on :x in PRINT :x (line 1, col 6)
        val target = findDecl(source, 1, 6)
        // Should jump to "x in MAKE "x (line 0, col 5)
        assertEquals(0 to 5, target)
    }

    @Test
    fun `local shadows global`() {
        val source = "MAKE \"x 10\nTO test\n  LOCALMAKE \"x 5\n  PRINT :x\nEND"
        // Click on :x in PRINT :x (line 3, col 8)
        val target = findDecl(source, 3, 8)
        // Should jump to LOCALMAKE "x (line 2, col 12), not global
        assertEquals(2 to 12, target)
    }

    @Test
    fun `local declaration via LOCAL`() {
        val source = "TO test\n  LOCAL \"a\n  MAKE \"a 5\n  PRINT :a\nEND"
        // Click on :a in PRINT :a (line 3, col 8)
        val target = findDecl(source, 3, 8)
        // Should jump to LOCAL "a (line 1, col 8)
        assertEquals(1 to 8, target)
    }

    @Test
    fun `variable not found returns empty`() {
        val source = "PRINT :unknown"
        val target = findDecl(source, 0, 6)
        assertNull(target)
    }

    @Test
    fun `make after local goes to local not make`() {
        val source = "TO test\n  LOCAL \"a\n  MAKE \"a 5\n  PRINT :a\nEND"
        // Click on :a in PRINT :a (line 3)
        val target = findDecl(source, 3, 8)
        // Should jump to LOCAL "a (line 1, col 8), NOT to MAKE "a (line 2)
        assertEquals(1 to 8, target)
        assertNotEquals(2 to 7, target)
    }

    @Test
    fun `make without local goes to make`() {
        val source = "TO test\n  MAKE \"a 5\n  PRINT :a\nEND"
        val target = findDecl(source, 2, 8)
        // No LOCAL — should go to MAKE "a (line 1, col 7)
        assertEquals(1 to 7, target)
    }

    @Test
    fun `case insensitive variable lookup`() {
        val source = "MAKE \"Size 5\nPRINT :size"
        val target = findDecl(source, 1, 6)
        assertEquals(0 to 5, target)
    }

    // -- Procedure declarations --

    @Test
    fun `goto procedure definition`() {
        val source = "TO square :size\n  REPEAT 4 [FD :size RT 90]\nEND\nsquare 100"
        // Click on square in "square 100" (line 3, col 0)
        val target = findDecl(source, 3, 0)
        // Should jump to "square" in TO square (line 0, col 3)
        assertEquals(0 to 3, target)
    }

    @Test
    fun `goto procedure forward reference`() {
        val source = "draw\nTO draw\n  FD 100\nEND"
        // Click on draw call (line 0, col 0)
        val target = findDecl(source, 0, 0)
        // Should jump to "draw" in TO draw (line 1, col 3)
        assertEquals(1 to 3, target)
    }

    @Test
    fun `procedure not found returns empty`() {
        val source = "unknown"
        val target = findDecl(source, 0, 0)
        assertNull(target)
    }

    @Test
    fun `case insensitive procedure lookup`() {
        val source = "TO MyProc\n  FD 100\nEND\nmyproc"
        val target = findDecl(source, 3, 0)
        assertEquals(0 to 3, target)
    }
}
