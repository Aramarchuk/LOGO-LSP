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

    /**
     * Find the declaration location for a symbol at the given position.
     *
     * Resolves:
     * - :varname → finds MAKE "varname or procedure parameter definition
     * - procedureName → finds TO definition
     *
     * Returns a list of Location objects (usually 0 or 1 item).
     */
    fun findDeclaration(uri: String, position: Position, parseResult: Parser.ParseResult): List<Location> {
        val path = parseResult.program.findNodePath(position.line, position.character).reversed()
        val deepest = path.firstOrNull()
        val declToken: Token? = when (deepest) {
            is VariableRef -> findVariableDeclaration(deepest, path)
            is CommandCall -> findProcedureDeclaration(deepest.nameToken, path)
            else -> null
        }

        return if (declToken != null) listOf(tokenToLocation(uri, declToken)) else emptyList()
    }

    /**
     * Create a Location object from a token and document URI.
     */
    private fun tokenToLocation(uri: String, token: Token): Location {
        return Location(uri, Range(
            Position(token.line, token.column),
            Position(token.line, token.column + token.length),
        ))
    }

    /**
     * Find where a variable is declared.
     * Priority: LOCAL/LOCALMAKE in procedure > parameter > any MAKE in procedure > any MAKE anywhere.
     * Path is reversed (deepest first).
     */
    private fun findVariableDeclaration(ref: VariableRef, reversedPath: List<Node>): Token? {
        val name = ref.token.text.removePrefix(":").uppercase(Locale.ROOT)
        val proc = reversedPath.filterIsInstance<ProcedureDefinition>().firstOrNull()
        val program = reversedPath.filterIsInstance<Program>().firstOrNull() ?: return null

        if (proc != null) {
            // 1. LOCAL / LOCALMAKE in procedure
            for (node in proc.walk()) {
                when (node) {
                    is VariableAssignment -> {
                        if (node.local && node.nameToken.text.removePrefix("\"").uppercase(Locale.ROOT) == name) {
                            return node.nameToken
                        }
                    }
                    is LocalDeclaration -> {
                        val match = node.names.firstOrNull {
                            it.text.removePrefix("\"").uppercase(Locale.ROOT) == name
                        }
                        if (match != null) return match
                    }
                    else -> {}
                }
            }

            // 2. Procedure parameter
            val param = proc.params.firstOrNull {
                it.text.removePrefix(":").uppercase(Locale.ROOT) == name
            }
            if (param != null) return param

            // 3. Any MAKE in procedure (non-local — creates global at runtime)
            for (node in proc.walk()) {
                if (node is VariableAssignment && !node.local
                    && node.nameToken.text.removePrefix("\"").uppercase(Locale.ROOT) == name) {
                    return node.nameToken
                }
            }
        }

        // 4. Any MAKE anywhere in the program, excluding variables declared local in their procedure
        val localNames = collectLocalNames(program)
        for (node in program.walk()) {
            if (node is VariableAssignment
                && node.nameToken.text.removePrefix("\"").uppercase(Locale.ROOT) == name
                && node !in localNames) {
                return node.nameToken
            }
        }

        return null
    }

    /**
     * Collect all VariableAssignments that assign to a variable declared local
     * (via LOCAL or LOCALMAKE) in the same procedure, or are LOCALMAKE themselves.
     */
    private fun collectLocalNames(program: Program): Set<VariableAssignment> {
        val localAssignments = mutableSetOf<VariableAssignment>()
        for (node in program.walk()) {
            if (node is ProcedureDefinition) {
                // Find names declared local in this procedure
                val declaredLocal = mutableSetOf<String>()
                for (inner in node.walk()) {
                    when (inner) {
                        is VariableAssignment -> {
                            if (inner.local) {
                                declaredLocal += inner.nameToken.text.removePrefix("\"").uppercase(Locale.ROOT)
                                localAssignments += inner
                            }
                        }
                        is LocalDeclaration -> {
                            inner.names.mapTo(declaredLocal) { it.text.removePrefix("\"").uppercase(Locale.ROOT) }
                        }
                        else -> {}
                    }
                }
                // Mark any MAKE to a declared-local name as local too
                for (inner in node.walk()) {
                    if (inner is VariableAssignment && !inner.local
                        && inner.nameToken.text.removePrefix("\"").uppercase(Locale.ROOT) in declaredLocal) {
                        localAssignments += inner
                    }
                }
            }
        }
        return localAssignments
    }

    /**
     * Find where a procedure is defined (TO name ...).
     */
    private fun findProcedureDeclaration(nameToken: Token, reversedPath: List<Node>): Token? {
        val name = nameToken.text.uppercase(Locale.ROOT)
        val program = reversedPath.filterIsInstance<Program>().firstOrNull() ?: return null

        return program.walk()
            .filterIsInstance<ProcedureDefinition>()
            .firstOrNull { it.nameToken.text.uppercase(Locale.ROOT) == name }
            ?.nameToken
    }
}
