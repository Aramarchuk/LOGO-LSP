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
        val statements = parseStatementList(TokenType.EOF, topLevel = true)
        return ParseResult(Program(statements), errors.toList())
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
            TokenType.STOP -> { StopStatement(advance()) }
            TokenType.MAKE -> parseMake(local = false)
            TokenType.LOCALMAKE -> parseMake(local = true)
            TokenType.LOCAL -> parseLocal()
            TokenType.WORD -> parseCommandCallStatement()
            else -> {
                val tok = current()
                errors += ParseError(tok, "Unexpected token: '${tok.text}'")
                advance()
                ErrorNode(tok, "Unexpected token")
            }
        }
    }

    private fun parseProcedureDef(): Node {
        val toToken = advance() // TO
        skipNewlines()
        val nameToken = expect(TokenType.WORD, "Expected procedure name after TO")
        if (nameToken.type != TokenType.WORD) {
            recover()
            return ErrorNode(toToken, "Expected procedure name after TO")
        }
        val params = mutableListOf<Token>()
        while (current().type == TokenType.VARIABLE_REF) {
            params += advance()
        }
        skipNewlines()
        val body = parseStatementList(TokenType.END, topLevel = false)
        if (current().type == TokenType.END) {
            advance()
        } else {
            errors += ParseError(current(), "Expected END to close procedure '${nameToken.text}'")
        }
        return ProcedureDefinition(toToken, nameToken, params, body)
    }

    private fun parseRepeat(): Node {
        val keyword = advance()
        val count = parseExpression()
        val body = parseBlock()
        return RepeatStatement(keyword, count, body)
    }

    private fun parseIf(hasElse: Boolean): Node {
        val keyword = advance()
        val condition = parseExpression()
        val thenBody = parseBlock()
        val elseBody = if (hasElse) parseBlock() else null
        return IfStatement(keyword, condition, thenBody, elseBody)
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
        val body = parseBlock()
        return WhileStatement(keyword, condition, body)
    }

    private fun parseFor(): Node {
        val keyword = advance()
        expect(TokenType.LBRACKET, "Expected '[' for FOR control list")
        val varName = expect(TokenType.WORD, "Expected variable name in FOR")
        val from = parseExpression()
        val to = parseExpression()
        val step = if (current().type != TokenType.RBRACKET) parseExpression() else null
        expect(TokenType.RBRACKET, "Expected ']' after FOR control list")
        val body = parseBlock()
        return ForStatement(keyword, varName, from, to, step, body)
    }

    private fun parseForEach(): Node {
        val keyword = advance()
        val list = parseExpression()
        val body = parseBlock()
        return ForEachStatement(keyword, list, body)
    }

    private fun parseForever(): Node {
        val keyword = advance()
        val body = parseBlock()
        return ForeverStatement(keyword, body)
    }

    private fun parseOutput(): Node {
        val keyword = advance()
        val value = parseExpression()
        return OutputStatement(keyword, value)
    }

    private fun parseMake(local: Boolean): Node {
        val keyword = advance()
        if (current().type == TokenType.QUOTED_WORD) {
            val nameToken = advance()
            val value = parseExpression()
            return VariableAssignment(keyword, nameToken, value, local)
        }
        // Non-standard form — parse as a 2-arg command call
        val arg1 = parseExpression()
        val arg2 = parseExpression()
        return CommandCall(keyword, listOf(arg1, arg2))
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
        return LocalDeclaration(keyword, names)
    }

    private fun parseCommandCallStatement(): Node {
        val nameToken = advance()
        val arity = lookupArity(nameToken.text)
        val args = (1..arity).map { parseExpression() }
        return CommandCall(nameToken, args)
    }

    // -- Block parsing --

    private fun parseBlock(): List<Node> {
        if (current().type != TokenType.LBRACKET) {
            errors += ParseError(current(), "Expected '['")
            return emptyList()
        }
        advance() // skip [
        skipNewlines()
        val stmts = parseStatementList(TokenType.RBRACKET, topLevel = false)
        if (current().type == TokenType.RBRACKET) {
            advance()
        } else {
            errors += ParseError(current(), "Expected ']'")
        }
        return stmts
    }

    // -- Expression parsing (full, used for statement-level command args) --

    private fun parseExpression(): Node = parseOr()

    private fun parseOr(): Node {
        var left = parseAnd()
        while (current().type == TokenType.OR) {
            val op = advance()
            left = BinaryExpr(left, op, parseAnd())
        }
        return left
    }

    private fun parseAnd(): Node {
        var left = parseComparison()
        while (current().type == TokenType.AND) {
            val op = advance()
            left = BinaryExpr(left, op, parseComparison())
        }
        return left
    }

    private fun parseComparison(): Node {
        var left = parseAddSub()
        val type = current().type
        if (type in COMPARISON_OPS) {
            val op = advance()
            left = BinaryExpr(left, op, parseAddSub())
        }
        return left
    }

    private fun parseAddSub(): Node {
        var left = parseMulDiv()
        while (current().type in ADDITIVE_OPS) {
            val op = advance()
            left = BinaryExpr(left, op, parseMulDiv())
        }
        return left
    }

    private fun parseMulDiv(): Node {
        var left = parseUnary()
        while (current().type in MULTIPLICATIVE_OPS) {
            val op = advance()
            left = BinaryExpr(left, op, parseUnary())
        }
        return left
    }

    // -- Tight expression (used for reporter args inside expressions) --

    private fun parseTightExpression(): Node = parseUnary()

    private fun parseUnary(): Node {
        if (current().type == TokenType.MINUS) {
            val op = advance()
            return UnaryExpr(op, parseUnary())
        }
        if (current().type == TokenType.NOT) {
            val op = advance()
            return UnaryExpr(op, parseUnary())
        }
        return parsePrimary()
    }

    private fun parsePrimary(): Node {
        return when (current().type) {
            TokenType.NUMBER -> NumberLiteral(advance())
            TokenType.VARIABLE_REF -> VariableRef(advance())
            TokenType.QUOTED_WORD -> WordLiteral(advance())
            TokenType.BOOLEAN_TRUE, TokenType.BOOLEAN_FALSE -> BooleanLiteral(advance())
            TokenType.LBRACKET -> parseListLiteral()
            TokenType.LPAREN -> parseParenExpr()
            TokenType.WORD -> parseReporterCall()
            // Boundary tokens — don't advance, let the caller handle them
            TokenType.NEWLINE, TokenType.EOF, TokenType.RBRACKET, TokenType.RPAREN, TokenType.END -> {
                val tok = current()
                errors += ParseError(tok, "Expected expression")
                ErrorNode(tok, "Expected expression")
            }
            else -> {
                val tok = current()
                errors += ParseError(tok, "Expected expression, got '${tok.text}'")
                advance()
                ErrorNode(tok, "Expected expression")
            }
        }
    }

    private fun parseReporterCall(): CommandCall {
        val nameToken = advance()
        val arity = lookupArity(nameToken.text)
        val args = (1..arity).map { parseTightExpression() }
        return CommandCall(nameToken, args)
    }

    private fun parseListLiteral(): ListLiteral {
        val open = advance() // [
        val elements = mutableListOf<Node>()
        while (current().type != TokenType.RBRACKET && current().type != TokenType.EOF) {
            when (current().type) {
                TokenType.NEWLINE, TokenType.COMMENT -> advance()
                TokenType.NUMBER -> elements += NumberLiteral(advance())
                TokenType.VARIABLE_REF -> elements += VariableRef(advance())
                TokenType.BOOLEAN_TRUE, TokenType.BOOLEAN_FALSE -> elements += BooleanLiteral(advance())
                TokenType.LBRACKET -> elements += parseListLiteral()
                else -> elements += WordLiteral(advance())
            }
        }
        if (current().type == TokenType.RBRACKET) {
            advance()
        } else {
            errors += ParseError(current(), "Expected ']'")
        }
        return ListLiteral(open, elements)
    }

    private fun parseParenExpr(): Node {
        val open = advance() // (
        val expr = parseExpression()
        if (current().type == TokenType.RPAREN) {
            advance()
        } else {
            errors += ParseError(current(), "Expected ')'")
        }
        return ParenExpr(open, expr)
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
