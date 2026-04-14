package logo.features

import logo.lexer.Token
import logo.parser.CommandCall
import logo.parser.LocalDeclaration
import logo.parser.Node
import logo.parser.Parser
import logo.parser.ProcedureDefinition
import logo.parser.Program
import logo.parser.VariableAssignment
import logo.parser.VariableRef
import logo.parser.findNodePath
import logo.parser.walk
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.util.Locale

object GoToDeclarationProvider {

    private data class ProcedureVariableCandidates(
        var firstLocalMatch: Token? = null,
        var firstMakeMatch: Token? = null,
    )

    fun findDeclaration(uri: String, position: Position, parseResult: Parser.ParseResult): List<Location> {
        val path = parseResult.program.findNodePath(position.line, position.character)
        val nodeAtCursor = path.lastOrNull()
        val declToken: Token? = when (nodeAtCursor) {
            is VariableRef -> findVariableDeclaration(nodeAtCursor, path)
            is CommandCall -> findProcedureDeclaration(nodeAtCursor.nameToken, parseResult.program)
            else -> null
        }
        return if (declToken != null) listOf(tokenToLocation(uri, declToken)) else emptyList()
    }

    // -- Variable resolution: walk path from cursor to root --

    private fun findVariableDeclaration(ref: VariableRef, path: List<Node>): Token? {
        val name = ref.token.normalizedVariableRefName()
        for (node in path.asReversed()) {
            when (node) {
                is ProcedureDefinition -> findVariableInProcedure(name, node)?.let { return it }
                is Program -> findTopLevelMake(name, node)?.let { return it }
                else -> {}
            }
        }
        return null
    }

    private fun findVariableInProcedure(name: String, proc: ProcedureDefinition): Token? {
        val candidates = collectProcedureVariableCandidates(name, proc)
        return candidates.firstLocalMatch ?: findParameter(name, proc) ?: candidates.firstMakeMatch
    }

    private fun collectProcedureVariableCandidates(name: String, proc: ProcedureDefinition): ProcedureVariableCandidates {
        val candidates = ProcedureVariableCandidates()
        for (node in proc.walk()) {
            when (node) {
                is VariableAssignment -> collectVariableAssignmentCandidate(name, node, candidates)
                is LocalDeclaration -> collectLocalDeclarationCandidate(name, node, candidates)
                else -> {}
            }
        }

        return candidates
    }

    private fun collectVariableAssignmentCandidate(
        name: String,
        assignment: VariableAssignment,
        candidates: ProcedureVariableCandidates,
    ) {
        if (assignment.nameToken.normalizedVariableWordName() != name) return

        if (assignment.local && candidates.firstLocalMatch == null) {
            candidates.firstLocalMatch = assignment.nameToken
        }
        if (!assignment.local && candidates.firstMakeMatch == null) {
            candidates.firstMakeMatch = assignment.nameToken
        }
    }

    private fun collectLocalDeclarationCandidate(
        name: String,
        declaration: LocalDeclaration,
        candidates: ProcedureVariableCandidates,
    ) {
        if (candidates.firstLocalMatch != null) return

        candidates.firstLocalMatch = declaration.names.firstOrNull { it.normalizedVariableWordName() == name }
    }

    private fun findParameter(name: String, proc: ProcedureDefinition): Token? {
        return proc.params.firstOrNull { it.normalizedVariableRefName() == name }
    }


    private fun findTopLevelMake(name: String, program: Program): Token? {
        return program.statements
            .filterIsInstance<VariableAssignment>()
            .firstOrNull { it.nameToken.normalizedVariableWordName() == name }
            ?.nameToken
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
