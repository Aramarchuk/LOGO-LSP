package logo.parser

import logo.lexer.Token

data class Span(val startLine: Int, val startCol: Int, val endLine: Int, val endCol: Int)

sealed interface Node {
    val span: Span
}

data class Program(val statements: List<Node>, override val span: Span) : Node

data class ProcedureDefinition(
    val toToken: Token,
    val nameToken: Token,
    val params: List<Token>,
    val body: List<Node>,
    override val span: Span,
) : Node

data class CommandCall(
    val nameToken: Token,
    val args: List<Node>,
    override val span: Span,
) : Node

data class VariableAssignment(
    val keyword: Token,
    val nameToken: Token,
    val value: Node,
    val local: Boolean,
    override val span: Span,
) : Node

data class LocalDeclaration(
    val keyword: Token,
    val names: List<Token>,
    override val span: Span,
) : Node

data class RepeatStatement(
    val keyword: Token,
    val count: Node,
    val body: List<Node>,
    override val span: Span,
) : Node

data class IfStatement(
    val keyword: Token,
    val condition: Node,
    val thenBody: List<Node>,
    val elseBody: List<Node>? = null,
    override val span: Span,
) : Node

data class WhileStatement(
    val keyword: Token,
    val condition: Node,
    val body: List<Node>,
    override val span: Span,
) : Node

data class ForStatement(
    val keyword: Token,
    val varName: Token,
    val from: Node,
    val to: Node,
    val step: Node?,
    val body: List<Node>,
    override val span: Span,
) : Node

data class ForEachStatement(
    val keyword: Token,
    val list: Node,
    val body: List<Node>,
    override val span: Span,
) : Node

data class ForeverStatement(
    val keyword: Token,
    val body: List<Node>,
    override val span: Span,
) : Node

data class OutputStatement(
    val keyword: Token,
    val value: Node,
    override val span: Span,
) : Node

data class StopStatement(
    val keyword: Token,
    override val span: Span,
) : Node

data class NumberLiteral(val token: Token, override val span: Span) : Node

data class WordLiteral(val token: Token, override val span: Span) : Node

data class BooleanLiteral(val token: Token, override val span: Span) : Node

data class VariableRef(val token: Token, override val span: Span) : Node

data class ListLiteral(
    val openBracket: Token,
    val elements: List<Node>,
    override val span: Span,
) : Node

data class BinaryExpr(
    val left: Node,
    val op: Token,
    val right: Node,
    override val span: Span,
) : Node

data class UnaryExpr(
    val op: Token,
    val operand: Node,
    override val span: Span,
) : Node

data class ParenExpr(
    val openParen: Token,
    val expr: Node,
    override val span: Span,
) : Node

data class ErrorNode(
    val token: Token,
    val message: String,
    override val span: Span,
) : Node

/** Direct child nodes of this AST node. */
fun Node.children(): List<Node> = when (this) {
    is Program -> statements
    is ProcedureDefinition -> body
    is CommandCall -> args
    is VariableAssignment -> listOf(value)
    is LocalDeclaration -> emptyList()
    is RepeatStatement -> listOf(count) + body
    is IfStatement -> listOf(condition) + thenBody + (elseBody ?: emptyList())
    is WhileStatement -> listOf(condition) + body
    is ForStatement -> listOfNotNull(from, to, step) + body
    is ForEachStatement -> listOf(list) + body
    is ForeverStatement -> body
    is OutputStatement -> listOf(value)
    is StopStatement -> emptyList()
    is NumberLiteral -> emptyList()
    is WordLiteral -> emptyList()
    is BooleanLiteral -> emptyList()
    is VariableRef -> emptyList()
    is ListLiteral -> elements
    is BinaryExpr -> listOf(left, right)
    is UnaryExpr -> listOf(operand)
    is ParenExpr -> listOf(expr)
    is ErrorNode -> emptyList()
}

/** Recursively walk all nodes in the AST (depth-first, pre-order). */
fun Node.walk(): Sequence<Node> = sequence {
    yield(this@walk)
    for (child in children()) {
        yieldAll(child.walk())
    }
}

/** Find the path from root to the deepest node containing the given position. */
fun Node.findNodePath(line: Int, col: Int): List<Node> {
    if (!span.contains(line, col)) return emptyList()
    for (child in children()) {
        val childPath = child.findNodePath(line, col)
        if (childPath.isNotEmpty()) return listOf(this) + childPath
    }
    return listOf(this)
}

/** Check if this span contains the given position. */
fun Span.contains(line: Int, col: Int): Boolean {
    if (line < startLine || line > endLine) return false
    if (line == startLine && col < startCol) return false
    if (line == endLine && col > endCol) return false
    return true
}
