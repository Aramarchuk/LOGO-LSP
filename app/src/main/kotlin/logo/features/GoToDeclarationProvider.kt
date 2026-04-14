package logo.features

import logo.lexer.Token
import logo.parser.CommandCall
import logo.parser.ForEachStatement
import logo.parser.ForStatement
import logo.parser.ForeverStatement
import logo.parser.IfStatement
import logo.parser.LocalDeclaration
import logo.parser.Node
import logo.parser.Parser
import logo.parser.ProcedureDefinition
import logo.parser.Program
import logo.parser.RepeatStatement
import logo.parser.VariableAssignment
import logo.parser.VariableRef
import logo.parser.WhileStatement
import logo.parser.children
import logo.parser.findNodePath
import logo.parser.walk
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.util.Locale

object GoToDeclarationProvider {

    private data class ResolutionContext(
        val name: String,
        val path: List<Node>,
        val program: Program,
        val currentProcedure: ProcedureDefinition?,
    )

    fun findDeclaration(uri: String, position: Position, parseResult: Parser.ParseResult): List<Location> {
        val path = parseResult.program.findNodePath(position.line, position.character)
        val nodeAtCursor = path.lastOrNull()
        val declTokens: List<Token> = when (nodeAtCursor) {
            is VariableRef -> findVariableDeclarations(nodeAtCursor, path, parseResult.program)
            is CommandCall -> findProcedureDeclaration(nodeAtCursor.nameToken, parseResult.program)?.let(::listOf) ?: emptyList()
            else -> emptyList()
        }
        return declTokens.map { tokenToLocation(uri, it) }
    }

    // -- Variable resolution --

    private fun findVariableDeclarations(ref: VariableRef, path: List<Node>, program: Program): List<Token> {
        val context = ResolutionContext(
            name = ref.token.normalizedVariableRefName(),
            path = path,
            program = program,
            currentProcedure = path.filterIsInstance<ProcedureDefinition>().lastOrNull(),
        )

        val forMatch = findNearestForMatch(context)
        if (forMatch != null) return listOf(forMatch)

        val procedureMatches = findInCurrentProcedure(context)
        if (procedureMatches.isNotEmpty()) return procedureMatches

        val globalMatches = findInGlobalScope(context)
        if (globalMatches.isNotEmpty()) return globalMatches

        val otherProcedureMatches = findInOtherProcedures(context)
        if (otherProcedureMatches.isNotEmpty()) return otherProcedureMatches

        return emptyList()
    }

    private fun findNearestForMatch(context: ResolutionContext): Token? {
        return context.path.asReversed()
            .filterIsInstance<ForStatement>()
            .firstOrNull { it.varName.normalizedVariableWordName() == context.name }
            ?.varName
    }

    private fun findInCurrentProcedure(context: ResolutionContext): List<Token> {
        val proc = context.currentProcedure ?: return emptyList()

        val parameterMatch = findParameter(context.name, proc)
        if (parameterMatch != null) return listOf(parameterMatch)

        val definiteLocalMatch = findFirstTopLevelLocalMatch(context.name, proc.body)
        if (definiteLocalMatch != null) return listOf(definiteLocalMatch)

        return collectPossibleMatches(
            name = context.name,
            nodes = proc.body,
            allowLocal = true,
        )
    }

    private fun findInGlobalScope(context: ResolutionContext): List<Token> {
        val firstTopLevelGlobalMake = findFirstTopLevelMake(context.name, context.program.statements)
        if (firstTopLevelGlobalMake != null) return listOf(firstTopLevelGlobalMake)

        return collectPossibleMatches(
            name = context.name,
            nodes = context.program.statements,
            allowLocal = false,
        )
    }

    private fun findInOtherProcedures(context: ResolutionContext): List<Token> {
        val procedures = context.program.statements
            .filterIsInstance<ProcedureDefinition>()
            .filterNot { it === context.currentProcedure }

        val definiteMatches = mutableListOf<Token>()
        for (proc in procedures) {
            // LOCAL/LOCALMAKE makes the variable procedure-local; ignore this procedure in global fallback.
            if (findFirstTopLevelLocalMatch(context.name, proc.body) != null) continue

            val match = findFirstTopLevelMake(context.name, proc.body)
            if (match != null) definiteMatches += match
        }
        if (definiteMatches.isNotEmpty()) return sortAndDedupe(definiteMatches)

        val possibleMatches = mutableListOf<Token>()
        for (proc in procedures) {
            if (findFirstTopLevelLocalMatch(context.name, proc.body) != null) continue

            possibleMatches += collectPossibleMatches(
                name = context.name,
                nodes = proc.body,
                allowLocal = false,
            )
        }
        return sortAndDedupe(possibleMatches)
    }

    private fun findFirstTopLevelLocalMatch(name: String, body: List<Node>): Token? {
        for (stmt in body) {
            when (stmt) {
                is LocalDeclaration -> {
                    val localToken = stmt.names.firstOrNull { it.normalizedVariableWordName() == name }
                    if (localToken != null) return localToken
                }
                is VariableAssignment -> {
                    if (stmt.local && stmt.nameToken.normalizedVariableWordName() == name) {
                        return stmt.nameToken
                    }
                }
                else -> {}
            }
        }
        return null
    }

    private fun findFirstTopLevelMake(name: String, body: List<Node>): Token? {
        for (stmt in body) {
            if (stmt is VariableAssignment && !stmt.local && stmt.nameToken.normalizedVariableWordName() == name) {
                return stmt.nameToken
            }
        }
        return null
    }

    private fun collectPossibleMatches(
        name: String,
        nodes: List<Node>,
        allowLocal: Boolean,
    ): List<Token> {
        val matches = mutableListOf<Token>()
        for (node in nodes) {
            collectPossibleMatchesInNode(
                node = node,
                name = name,
                allowLocal = allowLocal,
                out = matches,
            )
        }
        return sortAndDedupe(matches)
    }

    private fun collectPossibleMatchesInNode(
        node: Node,
        name: String,
        allowLocal: Boolean,
        out: MutableList<Token>,
    ) {
        if (node is ProcedureDefinition) return


        when (node) {
            is LocalDeclaration -> {
                if (allowLocal) {
                    val localToken = node.names.firstOrNull { it.normalizedVariableWordName() == name }
                    if (localToken != null) out += localToken
                }
            }
            is VariableAssignment -> {
                if (node.nameToken.normalizedVariableWordName() == name) {
                    if (!node.local || allowLocal) out += node.nameToken
                }
            }
            is ForStatement -> {
                if (node.varName.normalizedVariableWordName() == name) {
                    out += node.varName
                }
            }
            else -> {}
        }

        for (child in node.children()) {
            collectPossibleMatchesInNode(
                node = child,
                name = name,
                allowLocal = allowLocal,
                out = out,
            )
        }
    }

    private fun sortAndDedupe(tokens: List<Token>): List<Token> {
        return tokens
            .distinctBy { Triple(it.line, it.column, it.text) }
            .sortedWith(compareBy<Token> { it.line }.thenBy { it.column })
    }

    private fun findParameter(name: String, proc: ProcedureDefinition): Token? {
        return proc.params.firstOrNull { it.normalizedVariableRefName() == name }
    }



    // -- Procedure resolution --

    private fun findProcedureDeclaration(nameToken: Token, program: Program): Token? {
        val name = nameToken.text.normalize()
        return program.walk()
            .filterIsInstance<ProcedureDefinition>()
            .firstOrNull { it.nameToken.text.normalize() == name }
            ?.nameToken
    }

    // -- Helpers --

    private fun String.normalize(): String = uppercase(Locale.ROOT)

    private fun Token.normalizedVariableRefName(): String = text.removePrefix(":").normalize()

    private fun Token.normalizedVariableWordName(): String = text.removePrefix("\"").normalize()

    private fun tokenToLocation(uri: String, token: Token): Location {
        return Location(uri, Range(
            Position(token.line, token.column),
            Position(token.line, token.column + token.length),
        ))
    }
}
