package logo.parser

import logo.lexer.Token

sealed interface Node

data class Program(val statements: List<Node>) : Node

data class ProcedureDefinition(
    val toToken: Token,
    val nameToken: Token,
    val params: List<Token>,
    val body: List<Node>,
) : Node

data class CommandCall(
    val nameToken: Token,
    val args: List<Node>,
) : Node

data class VariableAssignment(
    val keyword: Token,
    val nameToken: Token,
    val value: Node,
    val local: Boolean,
) : Node

data class LocalDeclaration(
    val keyword: Token,
    val names: List<Token>,
) : Node

data class RepeatStatement(
    val keyword: Token,
    val count: Node,
    val body: List<Node>,
) : Node

data class IfStatement(
    val keyword: Token,
    val condition: Node,
    val thenBody: List<Node>,
    val elseBody: List<Node>? = null,
) : Node

data class WhileStatement(
    val keyword: Token,
    val condition: Node,
    val body: List<Node>,
) : Node

data class ForStatement(
    val keyword: Token,
    val varName: Token,
    val from: Node,
    val to: Node,
    val step: Node?,
    val body: List<Node>,
) : Node

data class ForEachStatement(
    val keyword: Token,
    val list: Node,
    val body: List<Node>,
) : Node

data class ForeverStatement(
    val keyword: Token,
    val body: List<Node>,
) : Node

data class OutputStatement(
    val keyword: Token,
    val value: Node,
) : Node

data class StopStatement(
    val keyword: Token,
) : Node

data class NumberLiteral(val token: Token) : Node

data class WordLiteral(val token: Token) : Node

data class BooleanLiteral(val token: Token) : Node

data class VariableRef(val token: Token) : Node

data class ListLiteral(
    val openBracket: Token,
    val elements: List<Node>,
) : Node

data class BinaryExpr(
    val left: Node,
    val op: Token,
    val right: Node,
) : Node

data class UnaryExpr(
    val op: Token,
    val operand: Node,
) : Node

data class ParenExpr(
    val openParen: Token,
    val expr: Node,
) : Node

data class ErrorNode(
    val token: Token,
    val message: String,
) : Node
