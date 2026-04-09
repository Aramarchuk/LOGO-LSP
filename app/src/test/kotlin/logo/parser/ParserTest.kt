package logo.parser

import logo.lexer.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ParserTest {

    private fun parse(source: String): Parser.ParseResult {
        val tokens = Lexer(source).tokenize()
        return Parser(tokens).parse()
    }

    private fun parseOk(source: String): Program {
        val result = parse(source)
        assertTrue(result.errors.isEmpty(), "Expected no errors but got: ${result.errors}")
        return result.program
    }

    // -- Basic statements --

    @Test
    fun `empty program`() {
        val program = parseOk("")
        assertTrue(program.statements.isEmpty())
    }

    @Test
    fun `simple command call`() {
        val program = parseOk("FORWARD 100")
        assertEquals(1, program.statements.size)
        val call = program.statements[0] as CommandCall
        assertEquals("FORWARD", call.nameToken.text)
        assertEquals(1, call.args.size)
        assertEquals("100", (call.args[0] as NumberLiteral).token.text)
    }

    @Test
    fun `zero-arity command`() {
        val program = parseOk("PENUP")
        val call = program.statements[0] as CommandCall
        assertEquals("PENUP", call.nameToken.text)
        assertTrue(call.args.isEmpty())
    }

    @Test
    fun `two-arg command`() {
        val program = parseOk("SETXY 100 200")
        val call = program.statements[0] as CommandCall
        assertEquals("SETXY", call.nameToken.text)
        assertEquals(2, call.args.size)
        assertEquals("100", (call.args[0] as NumberLiteral).token.text)
        assertEquals("200", (call.args[1] as NumberLiteral).token.text)
    }

    @Test
    fun `multiple statements`() {
        val program = parseOk("FD 100\nRT 90\nFD 50")
        assertEquals(3, program.statements.size)
        assertEquals("FD", (program.statements[0] as CommandCall).nameToken.text)
        assertEquals("RT", (program.statements[1] as CommandCall).nameToken.text)
        assertEquals("FD", (program.statements[2] as CommandCall).nameToken.text)
    }

    @Test
    fun `command with alias`() {
        val program = parseOk("BK 50")
        val call = program.statements[0] as CommandCall
        assertEquals("BK", call.nameToken.text)
        assertEquals(1, call.args.size)
    }

    // -- Variable assignment --

    @Test
    fun `make assignment`() {
        val program = parseOk("MAKE \"size 100")
        val assign = program.statements[0] as VariableAssignment
        assertEquals("MAKE", assign.keyword.text)
        assertEquals("\"size", assign.nameToken.text)
        assertEquals("100", (assign.value as NumberLiteral).token.text)
        assertFalse(assign.local)
    }

    @Test
    fun `localmake assignment`() {
        val program = parseOk("LOCALMAKE \"x 42")
        val assign = program.statements[0] as VariableAssignment
        assertTrue(assign.local)
    }

    @Test
    fun `local declaration`() {
        val program = parseOk("LOCAL \"a \"b \"c")
        val decl = program.statements[0] as LocalDeclaration
        assertEquals(3, decl.names.size)
        assertEquals("\"a", decl.names[0].text)
        assertEquals("\"b", decl.names[1].text)
        assertEquals("\"c", decl.names[2].text)
    }

    // -- Procedure definition --

    @Test
    fun `procedure definition`() {
        val program = parseOk("""
            TO square :size
              REPEAT 4 [FD :size RT 90]
            END
        """.trimIndent())
        assertEquals(1, program.statements.size)
        val proc = program.statements[0] as ProcedureDefinition
        assertEquals("square", proc.nameToken.text)
        assertEquals(1, proc.params.size)
        assertEquals(":size", proc.params[0].text)
        assertEquals(1, proc.body.size)
    }

    @Test
    fun `procedure with no params`() {
        val program = parseOk("TO greet\nPRINT \"hello\nEND")
        val proc = program.statements[0] as ProcedureDefinition
        assertEquals("greet", proc.nameToken.text)
        assertTrue(proc.params.isEmpty())
        assertEquals(1, proc.body.size)
    }

    // -- Control structures --

    @Test
    fun `repeat statement`() {
        val program = parseOk("REPEAT 4 [FD 100 RT 90]")
        val repeat = program.statements[0] as RepeatStatement
        assertEquals("4", (repeat.count as NumberLiteral).token.text)
        assertEquals(2, repeat.body.size)
    }

    @Test
    fun `if statement`() {
        val program = parseOk("IF :x > 0 [PRINT \"positive]")
        val ifStmt = program.statements[0] as IfStatement
        val cond = ifStmt.condition as BinaryExpr
        assertTrue(cond.left is VariableRef)
        assertEquals("0", (cond.right as NumberLiteral).token.text)
        assertEquals(1, ifStmt.thenBody.size)
    }

    @Test
    fun `ifelse statement`() {
        val program = parseOk("IFELSE :x > 0 [PRINT \"yes] [PRINT \"no]")
        val ifElse = program.statements[0] as IfStatement
        assertTrue(ifElse.condition is BinaryExpr)
        assertEquals(1, ifElse.thenBody.size)
        assertNotNull(ifElse.elseBody)
        assertEquals(1, ifElse.elseBody!!.size)
    }

    @Test
    fun `while with bracketed condition`() {
        val program = parseOk("WHILE [:count > 0] [MAKE \"count :count - 1]")
        val whileStmt = program.statements[0] as WhileStatement
        assertTrue(whileStmt.condition is BinaryExpr)
        assertEquals(1, whileStmt.body.size)
    }

    @Test
    fun `while with direct condition`() {
        val program = parseOk("WHILE :count > 0 [MAKE \"count :count - 1]")
        val whileStmt = program.statements[0] as WhileStatement
        assertTrue(whileStmt.condition is BinaryExpr)
        assertEquals(1, whileStmt.body.size)
    }

    @Test
    fun `for statement`() {
        val program = parseOk("FOR [i 1 10] [PRINT :i]")
        val forStmt = program.statements[0] as ForStatement
        assertEquals("i", forStmt.varName.text)
        assertEquals("1", (forStmt.from as NumberLiteral).token.text)
        assertEquals("10", (forStmt.to as NumberLiteral).token.text)
        assertNull(forStmt.step)
        assertEquals(1, forStmt.body.size)
    }

    @Test
    fun `for statement with step`() {
        val program = parseOk("FOR [i 0 100 5] [PRINT :i]")
        val forStmt = program.statements[0] as ForStatement
        assertEquals("5", (forStmt.step as NumberLiteral).token.text)
    }

    @Test
    fun `foreach statement`() {
        val program = parseOk("FOREACH [1 2 3] [PRINT ?]")
        val forEach = program.statements[0] as ForEachStatement
        assertTrue(forEach.list is ListLiteral)
        assertEquals(1, forEach.body.size)
    }

    @Test
    fun `forever statement`() {
        val program = parseOk("FOREVER [FD 1 RT 1]")
        val forever = program.statements[0] as ForeverStatement
        assertEquals(2, forever.body.size)
    }

    @Test
    fun `output statement`() {
        val program = parseOk("TO double :x\nOUTPUT :x * 2\nEND")
        val proc = program.statements[0] as ProcedureDefinition
        val output = proc.body[0] as OutputStatement
        assertTrue(output.value is BinaryExpr)
    }

    @Test
    fun `stop statement`() {
        val program = parseOk("TO test\nIF :x > 10 [STOP]\nEND")
        val proc = program.statements[0] as ProcedureDefinition
        val ifStmt = proc.body[0] as IfStatement
        assertTrue(ifStmt.thenBody[0] is StopStatement)
    }

    // -- Expressions --

    @Test
    fun `arithmetic precedence`() {
        // PRINT 1 + 2 * 3  →  PRINT(1 + (2 * 3))
        val program = parseOk("PRINT 1 + 2 * 3")
        val call = program.statements[0] as CommandCall
        val expr = call.args[0] as BinaryExpr
        assertEquals("1", (expr.left as NumberLiteral).token.text)
        assertEquals("+", expr.op.text)
        val right = expr.right as BinaryExpr
        assertEquals("2", (right.left as NumberLiteral).token.text)
        assertEquals("*", right.op.text)
        assertEquals("3", (right.right as NumberLiteral).token.text)
    }

    @Test
    fun `parenthesized expression`() {
        val program = parseOk("PRINT (1 + 2) * 3")
        val call = program.statements[0] as CommandCall
        val expr = call.args[0] as BinaryExpr
        assertTrue(expr.left is ParenExpr)
        assertEquals("*", expr.op.text)
    }

    @Test
    fun `unary minus`() {
        val program = parseOk("PRINT -5")
        val call = program.statements[0] as CommandCall
        val expr = call.args[0] as UnaryExpr
        assertEquals("-", expr.op.text)
        assertEquals("5", (expr.operand as NumberLiteral).token.text)
    }

    @Test
    fun `not expression`() {
        val program = parseOk("IF NOT :done [PRINT \"working]")
        val ifStmt = program.statements[0] as IfStatement
        val cond = ifStmt.condition as UnaryExpr
        assertEquals("NOT", cond.op.text)
        assertTrue(cond.operand is VariableRef)
    }

    @Test
    fun `boolean literal`() {
        val program = parseOk("MAKE \"flag TRUE")
        val assign = program.statements[0] as VariableAssignment
        assertTrue(assign.value is BooleanLiteral)
    }

    @Test
    fun `comparison operators`() {
        val program = parseOk("IF :a <= :b [PRINT \"ok]")
        val ifStmt = program.statements[0] as IfStatement
        val cond = ifStmt.condition as BinaryExpr
        assertEquals("<=", cond.op.text)
    }

    @Test
    fun `logical operators`() {
        val program = parseOk("IF :a > 0 AND :b > 0 [PRINT \"both]")
        val ifStmt = program.statements[0] as IfStatement
        val cond = ifStmt.condition as BinaryExpr
        assertEquals("AND", cond.op.text)
        assertTrue(cond.left is BinaryExpr)
        assertTrue(cond.right is BinaryExpr)
    }

    @Test
    fun `variable ref in expression`() {
        val program = parseOk("FD :size")
        val call = program.statements[0] as CommandCall
        val arg = call.args[0] as VariableRef
        assertEquals(":size", arg.token.text)
    }

    // -- Tight expression for reporter args --

    @Test
    fun `reporter call with tight args`() {
        // PRINT SUM 3 4 + 5  →  PRINT((SUM 3 4) + 5) = PRINT(12)
        val program = parseOk("PRINT SUM 3 4 + 5")
        val print = program.statements[0] as CommandCall
        val expr = print.args[0] as BinaryExpr
        assertEquals("+", expr.op.text)

        val sum = expr.left as CommandCall
        assertEquals("SUM", sum.nameToken.text)
        assertEquals(2, sum.args.size)
        assertEquals("3", (sum.args[0] as NumberLiteral).token.text)
        assertEquals("4", (sum.args[1] as NumberLiteral).token.text)

        assertEquals("5", (expr.right as NumberLiteral).token.text)
    }

    @Test
    fun `nested reporter calls`() {
        // PRINT SUM RANDOM 100 50
        val program = parseOk("PRINT SUM RANDOM 100 50")
        val print = program.statements[0] as CommandCall
        val sum = print.args[0] as CommandCall
        assertEquals("SUM", sum.nameToken.text)

        val random = sum.args[0] as CommandCall
        assertEquals("RANDOM", random.nameToken.text)
        assertEquals("100", (random.args[0] as NumberLiteral).token.text)

        assertEquals("50", (sum.args[1] as NumberLiteral).token.text)
    }

    @Test
    fun `reporter in arithmetic expression`() {
        // FD RANDOM 100 + 50  →  FD(RANDOM(100) + 50)
        val program = parseOk("FD RANDOM 100 + 50")
        val fd = program.statements[0] as CommandCall
        val expr = fd.args[0] as BinaryExpr
        assertEquals("+", expr.op.text)

        val random = expr.left as CommandCall
        assertEquals("RANDOM", random.nameToken.text)
        assertEquals("100", (random.args[0] as NumberLiteral).token.text)

        assertEquals("50", (expr.right as NumberLiteral).token.text)
    }

    // -- List literals --

    @Test
    fun `list literal in expression`() {
        val program = parseOk("MAKE \"colors [red green blue]")
        val assign = program.statements[0] as VariableAssignment
        val list = assign.value as ListLiteral
        assertEquals(3, list.elements.size)
        assertEquals("red", (list.elements[0] as WordLiteral).token.text)
    }

    @Test
    fun `nested list literal`() {
        val program = parseOk("MAKE \"matrix [[1 2] [3 4]]")
        val assign = program.statements[0] as VariableAssignment
        val outer = assign.value as ListLiteral
        assertEquals(2, outer.elements.size)
        val inner1 = outer.elements[0] as ListLiteral
        assertEquals(2, inner1.elements.size)
    }

    // -- Two-pass arity: forward references --

    @Test
    fun `forward reference to user procedure`() {
        val program = parseOk("""
            draw
            TO draw
              FD 100
            END
        """.trimIndent())
        assertEquals(2, program.statements.size)
        val call = program.statements[0] as CommandCall
        assertEquals("draw", call.nameToken.text)
        assertTrue(call.args.isEmpty())
    }

    @Test
    fun `user procedure with args resolved via two-pass`() {
        val program = parseOk("""
            myproc 10 20
            TO myproc :a :b
              PRINT :a
            END
        """.trimIndent())
        val call = program.statements[0] as CommandCall
        assertEquals("myproc", call.nameToken.text)
        assertEquals(2, call.args.size)
    }

    // -- Error recovery --

    @Test
    fun `error on unexpected token does not crash`() {
        val result = parse("] FD 100")
        assertTrue(result.errors.isNotEmpty())
        // Parser should recover and still parse FD 100
        val stmts = result.program.statements
        assertTrue(stmts.any { it is CommandCall && it.nameToken.text == "FD" })
    }

    @Test
    fun `missing END in procedure`() {
        val result = parse("TO test\nFD 100")
        assertTrue(result.errors.isNotEmpty())
        val proc = result.program.statements[0] as ProcedureDefinition
        assertEquals("test", proc.nameToken.text)
    }

    // -- Complex programs --

    @Test
    fun `recursive spiral`() {
        val program = parseOk("""
            TO spiral :size
              IF :size > 100 [STOP]
              FD :size
              RT 90
              spiral :size + 5
            END
            spiral 5
        """.trimIndent())
        assertEquals(2, program.statements.size)
        val proc = program.statements[0] as ProcedureDefinition
        assertEquals("spiral", proc.nameToken.text)
        assertEquals(1, proc.params.size)
        assertEquals(4, proc.body.size)

        // The call after END uses arity from pass 1
        val call = program.statements[1] as CommandCall
        assertEquals("spiral", call.nameToken.text)
        assertEquals(1, call.args.size)
    }

    @Test
    fun `case insensitive procedure call`() {
        val program = parseOk("""
            TO MyProc :x
              PRINT :x
            END
            myproc 42
        """.trimIndent())
        val call = program.statements[1] as CommandCall
        assertEquals("myproc", call.nameToken.text)
        assertEquals(1, call.args.size)
    }

    @Test
    fun `comments are skipped`() {
        val program = parseOk("""
            ; this is a comment
            FD 100 ; inline comment
            ; another comment
            RT 90
        """.trimIndent())
        assertEquals(2, program.statements.size)
    }

    @Test
    fun `if without else has null elseBody`() {
        val program = parseOk("IF :x > 0 [PRINT \"yes]")
        val ifStmt = program.statements[0] as IfStatement
        assertNull(ifStmt.elseBody)
    }

    // -- Additional edge cases --

    @Test
    fun `empty procedure body`() {
        val program = parseOk("TO empty\nEND")
        val proc = program.statements[0] as ProcedureDefinition
        assertTrue(proc.body.isEmpty())
    }

    @Test
    fun `missing closing bracket`() {
        val result = parse("REPEAT 4 [FD 100 RT 90")
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `make without value`() {
        val result = parse("MAKE \"x")
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `dot is not part of word`() {
        val program = parseOk("FD.5")
        // FD is a 1-arg command, .5 is a number
        val call = program.statements[0] as CommandCall
        assertEquals("FD", call.nameToken.text)
        assertEquals(".5", (call.args[0] as NumberLiteral).token.text)
    }
}
