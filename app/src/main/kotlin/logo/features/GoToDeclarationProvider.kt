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
        val name = ref.token.text.removePrefix(":").normalize()
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
        return findLocalDeclaration(name, proc)
            ?: findParameter(name, proc)
            ?: findMakeInProcedure(name, proc)
    }

    private fun findLocalDeclaration(name: String, proc: ProcedureDefinition): Token? {
        for (node in proc.walk()) {
            when (node) {
                is VariableAssignment -> {
                    if (node.local && node.nameToken.text.removePrefix("\"").normalize() == name)
                        return node.nameToken
                }
                is LocalDeclaration -> {
                    val match = node.names.firstOrNull { it.text.removePrefix("\"").normalize() == name }
                    if (match != null) return match
                }
                else -> {}
            }
        }
        return null
    }

    private fun findParameter(name: String, proc: ProcedureDefinition): Token? {
        return proc.params.firstOrNull { it.text.removePrefix(":").normalize() == name }
    }

    private fun findMakeInProcedure(name: String, proc: ProcedureDefinition): Token? {
        return proc.walk()
            .filterIsInstance<VariableAssignment>()
            .firstOrNull { it.nameToken.text.removePrefix("\"").normalize() == name }
            ?.nameToken
    }

    private fun findTopLevelMake(name: String, program: Program): Token? {
        return program.statements
            .filterIsInstance<VariableAssignment>()
            .firstOrNull { it.nameToken.text.removePrefix("\"").normalize() == name }
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

    private fun tokenToLocation(uri: String, token: Token): Location {
        return Location(uri, Range(
            Position(token.line, token.column),
            Position(token.line, token.column + token.length),
        ))
    }
}
