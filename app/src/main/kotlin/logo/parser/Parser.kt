package logo.parser

import logo.analysis.BuiltinCommands
import logo.lexer.Token
import logo.lexer.TokenType
import java.util.Locale

class Parser(private val tokens: List<Token>) {

    data class ParseError(val token: Token, val message: String)
    data class ParseResult(val program: Program, val errors: List<ParseError>)

    private var pos = 0
    private val errors = mutableListOf<ParseError>()
    private val procedureArities = mutableMapOf<String, Int>()

    fun parse(): ParseResult {
        scanProcedureArities()
        pos = 0
        val start = current()
        val statements = parseStatementList(TokenType.EOF, topLevel = true)
        val span = if (statements.isEmpty()) {
            Span(start.line, start.column, start.line, start.column)
        } else {
            val lastStmt = statements.last()
            Span(start.line, start.column, lastStmt.span.endLine, lastStmt.span.endCol)
        }
        return ParseResult(Program(statements, span), errors.toList())
    }

    // -- Pass 1: scan TO...END to learn procedure arities --

    private fun scanProcedureArities() {
        var i = 0
        var bracketDepth = 0
        while (i < tokens.size) {
            when (tokens[i].type) {
                TokenType.LBRACKET -> bracketDepth++
                TokenType.RBRACKET -> if (bracketDepth > 0) bracketDepth--
                TokenType.TO -> if (bracketDepth == 0) {
                    i++
                    while (i < tokens.size && tokens[i].type in SKIP_TOKENS) i++
                    if (i < tokens.size && tokens[i].type == TokenType.WORD) {
                        val name = tokens[i].text.uppercase(Locale.ROOT)
                        i++
                        var arity = 0
                        while (i < tokens.size && tokens[i].type == TokenType.VARIABLE_REF) {
                            arity++
                            i++
                        }
                        procedureArities[name] = arity
                    }
                }
                else -> {}
            }
            i++
        }
    }

    // -- Token helpers --

    private fun current(): Token = tokens[pos]

    private fun advance(): Token {
        val tok = tokens[pos]
        if (pos < tokens.size - 1) pos++
        return tok
    }

    private fun skipNewlines() {
        while (current().type == TokenType.NEWLINE || current().type == TokenType.COMMENT) {
            advance()
        }
    }

    private fun expect(type: TokenType, message: String): Token {
        if (current().type == type) return advance()
        errors += ParseError(current(), message)
        return current()
    }

    private fun lookupArity(name: String): Int {
        val upper = name.uppercase(Locale.ROOT)
        return BuiltinCommands.lookup(upper)?.arity
            ?: procedureArities[upper]
            ?: 0
    }

    // -- Span helpers --

    private fun spanTo(start: Token, end: Token): Span {
        return Span(start.line, start.column, end.line, end.column + end.length)
    }

    private fun spanOf(token: Token): Span {
        return Span(token.line, token.column, token.line, token.column + token.length)
    }

    // -- Statement parsing --

    private fun parseStatementList(endType: TokenType, topLevel: Boolean): List<Node> {
        val stmts = mutableListOf<Node>()
        skipNewlines()
        while (current().type != endType && current().type != TokenType.EOF) {
            stmts += parseStatement(topLevel)
            skipNewlines()
        }
        return stmts
    }

    private fun parseStatement(topLevel: Boolean): Node {
        while (current().type == TokenType.COMMENT) advance()

        return when (current().type) {
            TokenType.TO -> {
                if (!topLevel) {
                    errors += ParseError(current(), "Procedure definitions must be at top level")
                }
                parseProcedureDef()
            }
            TokenType.REPEAT -> parseRepeat()
            TokenType.IF -> parseIf(hasElse = false)
            TokenType.IFELSE -> parseIf(hasElse = true)
            TokenType.WHILE -> parseWhile()
            TokenType.FOR -> parseFor()
            TokenType.FOREACH -> parseForEach()
            TokenType.FOREVER -> parseForever()
            TokenType.OUTPUT -> parseOutput()
            TokenType.STOP -> {
                val tok = advance()
                StopStatement(tok, spanOf(tok))
            }
            TokenType.MAKE -> parseMake(local = false)
            TokenType.LOCALMAKE -> parseMake(local = true)
            TokenType.LOCAL -> parseLocal()
            TokenType.WORD -> parseCommandCallStatement()
            else -> {
                val tok = current()
                errors += ParseError(tok, "Unexpected token: '${tok.text}'")
                advance()
                ErrorNode(tok, "Unexpected token", spanOf(tok))
            }
        }
    }

    private fun parseProcedureDef(): Node {
        val toToken = advance() // TO
        skipNewlines()
        val nameToken = expect(TokenType.WORD, "Expected procedure name after TO")
        if (nameToken.type != TokenType.WORD) {
            recover()
            return ErrorNode(toToken, "Expected procedure name after TO", spanOf(toToken))
        }
        val params = mutableListOf<Token>()
        while (current().type == TokenType.VARIABLE_REF) {
            params += advance()
        }
        skipNewlines()
        val body = parseStatementList(TokenType.END, topLevel = false)
        val endToken = if (current().type == TokenType.END) {
            advance()
        } else {
            errors += ParseError(current(), "Expected END to close procedure '${nameToken.text}'")
            current()
        }
        val span = Span(toToken.line, toToken.column, endToken.line, endToken.column + endToken.length)
        return ProcedureDefinition(toToken, nameToken, params, body, span)
    }

    private fun parseRepeat(): Node {
        val keyword = advance()
        val count = parseExpression()
        val block = parseBlock()
        return RepeatStatement(keyword, count, block.statements, spanTo(keyword, block.endToken))
    }

    private fun parseIf(hasElse: Boolean): Node {
        val keyword = advance()
        val condition = parseExpression()
        val thenBlock = parseBlock()
        val elseBlock = if (hasElse) parseBlock() else null
        val last = elseBlock?.endToken ?: thenBlock.endToken
        return IfStatement(keyword, condition, thenBlock.statements, elseBlock?.statements, spanTo(keyword, last))
    }

    private fun parseWhile(): Node {
        val keyword = advance()
        val condition = if (current().type == TokenType.LBRACKET) {
            advance() // skip [
            val expr = parseExpression()
            expect(TokenType.RBRACKET, "Expected ']' after WHILE condition")
            expr
        } else {
            parseExpression()
        }
        val block = parseBlock()
        return WhileStatement(keyword, condition, block.statements, spanTo(keyword, block.endToken))
    }

    private fun parseFor(): Node {
        val keyword = advance()
        expect(TokenType.LBRACKET, "Expected '[' for FOR control list")
        val varName = expect(TokenType.WORD, "Expected variable name in FOR")
        val from = parseExpression()
        val to = parseExpression()
        val step = if (current().type != TokenType.RBRACKET) parseExpression() else null
        expect(TokenType.RBRACKET, "Expected ']' after FOR control list")
        val block = parseBlock()
        return ForStatement(keyword, varName, from, to, step, block.statements, spanTo(keyword, block.endToken))
    }

    private fun parseForEach(): Node {
        val keyword = advance()
        val list = parseExpression()
        val block = parseBlock()
        return ForEachStatement(keyword, list, block.statements, spanTo(keyword, block.endToken))
    }

    private fun parseForever(): Node {
        val keyword = advance()
        val block = parseBlock()
        return ForeverStatement(keyword, block.statements, spanTo(keyword, block.endToken))
    }

    private fun parseOutput(): Node {
        val keyword = advance()
        val value = parseExpression()
        return OutputStatement(keyword, value, Span(keyword.line, keyword.column, value.span.endLine, value.span.endCol))
    }

    private fun parseMake(local: Boolean): Node {
        val keyword = advance()
        if (current().type == TokenType.QUOTED_WORD) {
            val nameToken = advance()
            val value = parseExpression()
            return VariableAssignment(keyword, nameToken, value, local, Span(keyword.line, keyword.column, value.span.endLine, value.span.endCol))
        }
        // Non-standard form — parse as a 2-arg command call
        val arg1 = parseExpression()
        val arg2 = parseExpression()
        return CommandCall(keyword, listOf(arg1, arg2), Span(keyword.line, keyword.column, arg2.span.endLine, arg2.span.endCol))
    }

    private fun parseLocal(): Node {
        val keyword = advance()
        val names = mutableListOf<Token>()
        while (current().type == TokenType.QUOTED_WORD) {
            names += advance()
        }
        if (names.isEmpty()) {
            errors += ParseError(current(), "Expected at least one variable name after LOCAL")
        }
        val lastNameOrKeyword = names.lastOrNull() ?: keyword
        return LocalDeclaration(keyword, names, Span(keyword.line, keyword.column, lastNameOrKeyword.line, lastNameOrKeyword.column + lastNameOrKeyword.length))
    }

    private fun parseCommandCallStatement(): Node {
        val nameToken = advance()
        val arity = lookupArity(nameToken.text)
        val args = (1..arity).map { parseExpression() }
        val (endLine, endCol) = if (args.isNotEmpty()) {
            args.last().span.endLine to args.last().span.endCol
        } else {
            nameToken.line to (nameToken.column + nameToken.length)
        }
        return CommandCall(nameToken, args, Span(nameToken.line, nameToken.column, endLine, endCol))
    }

    // -- Block parsing --

    private data class Block(val statements: List<Node>, val endToken: Token)

    private fun parseBlock(): Block {
        if (current().type != TokenType.LBRACKET) {
            errors += ParseError(current(), "Expected '['")
            return Block(emptyList(), current())
        }
        advance() // skip [
        skipNewlines()
        val stmts = parseStatementList(TokenType.RBRACKET, topLevel = false)
        val endToken = if (current().type == TokenType.RBRACKET) {
            advance()
        } else {
            errors += ParseError(current(), "Expected ']'")
            current()
        }
        return Block(stmts, endToken)
    }

    // -- Expression parsing (full, used for statement-level command args) --

    private fun parseExpression(): Node = parseOr()

    private fun parseOr(): Node {
        var left = parseAnd()
        while (current().type == TokenType.OR) {
            val op = advance()
            val right = parseAnd()
            left = BinaryExpr(left, op, right, Span(left.span.startLine, left.span.startCol, right.span.endLine, right.span.endCol))
        }
        return left
    }

    private fun parseAnd(): Node {
        var left = parseComparison()
        while (current().type == TokenType.AND) {
            val op = advance()
            val right = parseComparison()
            left = BinaryExpr(left, op, right, Span(left.span.startLine, left.span.startCol, right.span.endLine, right.span.endCol))
        }
        return left
    }

    private fun parseComparison(): Node {
        var left = parseAddSub()
        val type = current().type
        if (type in COMPARISON_OPS) {
            val op = advance()
            val right = parseAddSub()
            left = BinaryExpr(left, op, right, Span(left.span.startLine, left.span.startCol, right.span.endLine, right.span.endCol))
        }
        return left
    }

    private fun parseAddSub(): Node {
        var left = parseMulDiv()
        while (current().type in ADDITIVE_OPS) {
            val op = advance()
            val right = parseMulDiv()
            left = BinaryExpr(left, op, right, Span(left.span.startLine, left.span.startCol, right.span.endLine, right.span.endCol))
        }
        return left
    }

    private fun parseMulDiv(): Node {
        var left = parseUnary()
        while (current().type in MULTIPLICATIVE_OPS) {
            val op = advance()
            val right = parseUnary()
            left = BinaryExpr(left, op, right, Span(left.span.startLine, left.span.startCol, right.span.endLine, right.span.endCol))
        }
        return left
    }

    // -- Tight expression (used for reporter args inside expressions) --

    private fun parseTightExpression(): Node = parseUnary()

    private fun parseUnary(): Node {
        if (current().type == TokenType.MINUS) {
            val op = advance()
            val operand = parseUnary()
            return UnaryExpr(op, operand, Span(op.line, op.column, operand.span.endLine, operand.span.endCol))
        }
        if (current().type == TokenType.NOT) {
            val op = advance()
            val operand = parseUnary()
            return UnaryExpr(op, operand, Span(op.line, op.column, operand.span.endLine, operand.span.endCol))
        }
        return parsePrimary()
    }

    private fun parsePrimary(): Node {
        return when (current().type) {
            TokenType.NUMBER -> {
                val tok = advance()
                NumberLiteral(tok, spanOf(tok))
            }
            TokenType.VARIABLE_REF -> {
                val tok = advance()
                VariableRef(tok, spanOf(tok))
            }
            TokenType.QUOTED_WORD -> {
                val tok = advance()
                WordLiteral(tok, spanOf(tok))
            }
            TokenType.BOOLEAN_TRUE, TokenType.BOOLEAN_FALSE -> {
                val tok = advance()
                BooleanLiteral(tok, spanOf(tok))
            }
            TokenType.LBRACKET -> parseListLiteral()
            TokenType.LPAREN -> parseParenExpr()
            TokenType.WORD -> parseReporterCall()
            // Boundary tokens — don't advance, let the caller handle them
            TokenType.NEWLINE, TokenType.EOF, TokenType.RBRACKET, TokenType.RPAREN, TokenType.END -> {
                val tok = current()
                errors += ParseError(tok, "Expected expression")
                ErrorNode(tok, "Expected expression", spanOf(tok))
            }
            else -> {
                val tok = current()
                errors += ParseError(tok, "Expected expression, got '${tok.text}'")
                advance()
                ErrorNode(tok, "Expected expression", spanOf(tok))
            }
        }
    }

    private fun parseReporterCall(): CommandCall {
        val nameToken = advance()
        val arity = lookupArity(nameToken.text)
        val args = (1..arity).map { parseTightExpression() }
        val (endLine, endCol) = if (args.isNotEmpty()) {
            args.last().span.endLine to args.last().span.endCol
        } else {
            nameToken.line to (nameToken.column + nameToken.length)
        }
        return CommandCall(nameToken, args, Span(nameToken.line, nameToken.column, endLine, endCol))
    }

    private fun parseListLiteral(): ListLiteral {
        val open = advance() // [
        val elements = mutableListOf<Node>()
        while (current().type != TokenType.RBRACKET && current().type != TokenType.EOF) {
            when (current().type) {
                TokenType.NEWLINE, TokenType.COMMENT -> advance()
                TokenType.NUMBER -> {
                    val tok = advance()
                    elements += NumberLiteral(tok, spanOf(tok))
                }
                TokenType.VARIABLE_REF -> {
                    val tok = advance()
                    elements += VariableRef(tok, spanOf(tok))
                }
                TokenType.BOOLEAN_TRUE, TokenType.BOOLEAN_FALSE -> {
                    val tok = advance()
                    elements += BooleanLiteral(tok, spanOf(tok))
                }
                TokenType.LBRACKET -> elements += parseListLiteral()
                else -> {
                    val tok = advance()
                    elements += WordLiteral(tok, spanOf(tok))
                }
            }
        }
        val close = if (current().type == TokenType.RBRACKET) {
            advance()
        } else {
            errors += ParseError(current(), "Expected ']'")
            current()
        }
        val endLine = close.line
        val endCol = close.column + close.length
        return ListLiteral(open, elements, Span(open.line, open.column, endLine, endCol))
    }

    private fun parseParenExpr(): Node {
        val open = advance() // (
        val expr = parseExpression()
        val close = if (current().type == TokenType.RPAREN) {
            advance()
        } else {
            errors += ParseError(current(), "Expected ')'")
            current()
        }
        val endLine = close.line
        val endCol = close.column + close.length
        return ParenExpr(open, expr, Span(open.line, open.column, endLine, endCol))
    }

    // -- Error recovery --

    private fun recover() {
        while (current().type !in RECOVERY_TOKENS) {
            advance()
        }
    }

    companion object {
        private val SKIP_TOKENS = setOf(TokenType.NEWLINE, TokenType.COMMENT)
        private val ADDITIVE_OPS = setOf(TokenType.PLUS, TokenType.MINUS)
        private val MULTIPLICATIVE_OPS = setOf(TokenType.STAR, TokenType.SLASH)
        private val COMPARISON_OPS = setOf(
            TokenType.EQ, TokenType.LT, TokenType.GT,
            TokenType.LTE, TokenType.GTE, TokenType.NEQ,
        )
        private val RECOVERY_TOKENS = setOf(
            TokenType.NEWLINE, TokenType.END, TokenType.RBRACKET, TokenType.EOF,
        )
    }
}
