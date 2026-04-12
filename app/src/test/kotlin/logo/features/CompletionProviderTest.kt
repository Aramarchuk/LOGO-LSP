package logo.features

import logo.lexer.Lexer
import logo.parser.Parser
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CompletionProviderTest {

    private fun complete(source: String, line: Int, col: Int): List<String> {
        val tokens = Lexer(source).tokenize()
        val result = Parser(tokens).parse()
        return CompletionProvider.computeCompletions(Position(line, col), result)
            .map { it.label }
    }

    @Test
    fun `complete builtin command by prefix`() {
        val items = complete("FO", 0, 2)
        assertTrue(items.contains("FORWARD"))
    }

    @Test
    fun `complete keyword by prefix`() {
        val items = complete("FO", 0, 2)
        assertTrue(items.contains("FOR"))
        assertTrue(items.contains("FOREACH"))
        assertTrue(items.contains("FOREVER"))
    }

    @Test
    fun `empty prefix returns all commands and keywords`() {
        val items = complete("", 0, 0)
        assertTrue(items.size > 60) // ~60 builtins + 14 keywords
    }

    @Test
    fun `variable completion inside procedure`() {
        val source = """
            TO square :size
              MAKE "count 4
              FD :
            END
        """.trimIndent()
        val items = complete(source, 2, 5) // cursor on bare :
        assertTrue(items.contains("size"))
        assertTrue(items.contains("count"))
    }

    @Test
    fun `scoped variables - procedure params not visible outside`() {
        val source = """
            TO square :size
              FD :size
            END
            FD :s
        """.trimIndent()
        // cursor on :s outside any procedure — :size should not be suggested
        val items = complete(source, 3, 5)
        assertFalse(items.contains("size"))
    }

    @Test
    fun `global variables visible everywhere`() {
        val source = """
            MAKE "global 42
            TO test
              FD :g
            END
        """.trimIndent()
        val items = complete(source, 2, 6) // cursor on :g inside procedure
        assertTrue(items.contains("global"))
    }

    @Test
    fun `user-defined procedure in completions`() {
        val source = """
            TO myproc :x
              FD :x
            END
            my
        """.trimIndent()
        val items = complete(source, 3, 2) // cursor on "my"
        assertTrue(items.contains("myproc"))
    }

    @Test
    fun `case insensitive prefix matching`() {
        val items = complete("fo", 0, 2)
        assertTrue(items.contains("FORWARD"))
        assertTrue(items.contains("FOR"))
    }

    @Test
    fun `for loop variable in scope`() {
        val source = """
            FOR [i 1 10] [PRINT :]
        """.trimIndent()
        val items = complete(source, 0, 21) // cursor on bare :
        assertTrue(items.contains("i"))
    }

    @Test
    fun `for variable not visible outside its loop`() {
        val source = """
            FOR [i 1 10] [FD :i]
            FD :
        """.trimIndent()
        val items = complete(source, 1, 4)
        assertFalse(items.contains("i"))
    }

    @Test
    fun `other procedure variables not visible`() {
        val source = """
            TO procA :aa
              MAKE "localA 1
            END
            TO procB :bb
              FD :
            END
        """.trimIndent()
        val items = complete(source, 4, 6)
        assertTrue(items.contains("bb"))
        assertFalse(items.contains("aa"))
        assertFalse(items.contains("localA"))
    }

    @Test
    fun `nested for only inner variable visible`() {
        val source = """
            TO test
              FOR [i 1 10] [
                FOR [j 1 5] [
                  FD :
                ]
              ]
            END
        """.trimIndent()
        val items = complete(source, 3, 9) // cursor on : at col 9
        assertTrue(items.contains("j"))
        assertTrue(items.contains("i"))
    }
}
