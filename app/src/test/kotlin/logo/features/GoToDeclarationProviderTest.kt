package logo.features

import logo.lexer.Lexer
import logo.parser.Parser
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GoToDeclarationProviderTest {

    private fun findDecls(source: String, line: Int, col: Int): List<Pair<Int, Int>> {
        val tokens = Lexer(source).tokenize()
        val result = Parser(tokens).parse()
        return GoToDeclarationProvider.findDeclaration("test.logo", Position(line, col), result)
            .map { it.range.start.line to it.range.start.character }
    }

    // -- Variable declarations --

    @Test
    fun `goto procedure parameter`() {
        val source = "TO square :size\n  FD :size\nEND"
        val targets = findDecls(source, 1, 5)
        assertEquals(listOf(0 to 10), targets)
    }

    @Test
    fun `LOCAL-MAKE separated definition`() {
        val source = "TO square :size\n  LOCAL \"i \nMAKE \"i 10\nFD :size\nEND\nPRINT :i"
        val targets = findDecls(source, 5, 7)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun `goto global make`() {
        val source = "MAKE \"x 5\nPRINT :x"
        val targets = findDecls(source, 1, 6)
        assertEquals(listOf(0 to 5), targets)
    }

    @Test
    fun `local shadows global`() {
        val source = "MAKE \"x 10\nTO test\n  LOCALMAKE \"x 5\n  PRINT :x\nEND"
        val targets = findDecls(source, 3, 8)
        assertEquals(listOf(2 to 12), targets)
    }

    @Test
    fun `local declaration via LOCAL`() {
        val source = "TO test\n  LOCAL \"a\n  MAKE \"a 5\n  PRINT :a\nEND"
        val targets = findDecls(source, 3, 8)
        assertEquals(listOf(1 to 8), targets)
    }

    @Test
    fun `variable not found returns empty`() {
        val source = "PRINT :unknown"
        val targets = findDecls(source, 0, 6)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun `make after local goes to local not make`() {
        val source = "TO test\n  LOCAL \"a\n  MAKE \"a 5\n  PRINT :a\nEND"
        val targets = findDecls(source, 3, 8)
        assertEquals(listOf(1 to 8), targets)
    }

    @Test
    fun `procedure level make is not used before global and other-procedure fallback`() {
        val source = "TO test\n  MAKE \"a 5\n  PRINT :a\nEND"
        val targets = findDecls(source, 2, 8)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun `case insensitive variable lookup`() {
        val source = "MAKE \"Size 5\nPRINT :size"
        val targets = findDecls(source, 1, 6)
        assertEquals(listOf(0 to 5), targets)
    }

    @Test
    fun `inside for resolves to for header variable`() {
        val source = "FOR [i 1 3] [PRINT :i]"
        val targets = findDecls(source, 0, 19)
        assertEquals(listOf(0 to 5), targets)
    }

    @Test
    fun `outside for can resolve to global for variable in possible list`() {
        val source = "FOR [i 1 3] [PRINT :i]\nPRINT :i"
        val targets = findDecls(source, 1, 6)
        assertEquals(listOf(0 to 5), targets)
    }

    @Test
    fun `ifelse global make in both branches returns both`() {
        val source = "IFELSE 1=1 [MAKE \"x 1] [MAKE \"x 2]\nPRINT :x"
        val targets = findDecls(source, 1, 6)
        assertEquals(listOf(0 to 17, 0 to 29), targets)
    }

    @Test
    fun `ifelse procedure locals in both branches return both`() {
        val source = "TO test\n  IFELSE 1=1 [LOCALMAKE \"x 1] [LOCAL \"x]\n  PRINT :x\nEND"
        val targets = findDecls(source, 2, 8)
        assertEquals(listOf(1 to 24, 1 to 37), targets)
    }

    // -- Procedure declarations --

    @Test
    fun `goto procedure definition`() {
        val source = "TO square :size\n  REPEAT 4 [FD :size RT 90]\nEND\nsquare 100"
        val targets = findDecls(source, 3, 0)
        assertEquals(listOf(0 to 3), targets)
    }

    @Test
    fun `goto procedure forward reference`() {
        val source = "draw\nTO draw\n  FD 100\nEND"
        val targets = findDecls(source, 0, 0)
        assertEquals(listOf(1 to 3), targets)
    }

    @Test
    fun `procedure not found returns empty`() {
        val source = "unknown"
        val targets = findDecls(source, 0, 0)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun `case insensitive procedure lookup`() {
        val source = "TO MyProc\n  FD 100\nEND\nmyproc"
        val targets = findDecls(source, 3, 0)
        assertEquals(listOf(0 to 3), targets)
    }

    // -- Global fallback edge cases --

    @Test
    fun `localmake inside procedure not visible globally`() {
        val source = "TO test\n  LOCALMAKE \"x 5\nEND\nPRINT :x"
        val targets = findDecls(source, 3, 6)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun `make inside procedure is visible in other-procedure fallback`() {
        val source = "TO test\n  MAKE \"x 5\nEND\nPRINT :x"
        val targets = findDecls(source, 3, 6)
        assertEquals(listOf(1 to 7), targets)
    }

    @Test
    fun `other procedure fallback checks for locality`() {
        val source = "TO test\n  LOCAL \"y\n  MAKE \"y 5\nEND\nPRINT :y"
        val targets = findDecls(source, 4, 6)
        assertTrue(targets.isEmpty())
    }
    @Test
    fun `inside procedure fallback checks for locality`() {
        val source = "TO test\n  LOCAL \"y\n  MAKE \"y 5\nPRINT :y\nEND"
        val targets = findDecls(source, 3, 6)
        assertEquals(listOf(1 to 8), targets)
    }

    @Test
    fun `nested procedure variables are not visible in outer procedure`() {
        val source = "TO outer\n  TO inner\n    LOCALMAKE \"x 1\n  END\n  PRINT :x\nEND"
        val targets = findDecls(source, 4, 8)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun `test logo file case - global i resolves to for header`() {
        val source = "MAKE \"pointer 0\nFOR [i 10 10 5] [PRINT :i]\nPRINT :pointer\nTO square :sq\n    PRINT :sq\nEND\n\nPRINT :i\nWORD 1 1"
        val targets = findDecls(source, 7, 6)
        assertEquals(listOf(1 to 5), targets)
    }
}
