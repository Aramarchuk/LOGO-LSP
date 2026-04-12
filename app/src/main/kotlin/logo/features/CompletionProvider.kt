package logo.features

import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.Position
import logo.parser.Parser
import logo.parser.*
import logo.parser.walk
import logo.parser.children
import logo.parser.Span
import logo.analysis.BuiltinCommands
import java.util.Locale

object CompletionProvider {

    /**
     * Compute completion items at the given position.
     *
     * Context-aware completions:
     * - Variable reference context (:varname) → variable names from parse result
     * - Command/keyword context → builtin commands + user procedures + keywords
     *
     * Returns a list of CompletionItem objects.
     */
    fun computeCompletions(position: Position, parseResult: Parser.ParseResult): List<CompletionItem> {
        val program = parseResult.program
        val path = findNodePath(program, position.line, position.character)
        val deepestNode = path.lastOrNull()

        // Collect scoped variables from the parse result
        val variables = collectScopedVariables(path, program)

        // Determine context and suggest appropriate completions
        if (deepestNode is VariableRef) {
            // Suggest variable names, filter by prefix (strip leading :)
            val prefix = deepestNode.token.text.removePrefix(":")
            val completions = mutableListOf<CompletionItem>()
            for (varName in variables) {
                if (matchesPrefix(varName, prefix)) {
                    completions.add(variableCompletion(varName))
                }
            }
            return completions
        }

        // Command/keyword context
        val prefix = when (deepestNode) {
            is CommandCall -> deepestNode.nameToken.text
            is WordLiteral -> deepestNode.token.text
            else -> ""
        }

        val completions = mutableListOf<CompletionItem>()

        // Suggest builtin commands
        for (cmd in BuiltinCommands.all()) {
            if (matchesPrefix(cmd.name, prefix)) {
                completions.add(commandCompletion(cmd.name, cmd.arity))
            }
        }

        // Suggest user-defined procedures
        val procedures = collectProcedures(program)
        for (procName in procedures) {
            if (matchesPrefix(procName, prefix)) {
                completions.add(commandCompletion(procName, 0))
            }
        }

        // Suggest keywords
        val keywords = listOf(
            "TO", "END", "IF", "IFELSE", "REPEAT", "WHILE",
            "FOR", "FOREACH", "FOREVER", "MAKE", "LOCALMAKE",
            "LOCAL", "OUTPUT", "STOP"
        )
        for (keyword in keywords) {
            if (matchesPrefix(keyword, prefix)) {
                completions.add(keywordCompletion(keyword))
            }
        }

        return completions
    }

    /**
     * Find the path from root to the deepest AST node that contains the cursor position.
     * Returns a list of nodes where the last element is the deepest (most specific) node.
     */
    private fun findNodePath(node: Node, line: Int, col: Int): List<Node> {
        if (!contains(node.span, line, col)) return emptyList()
        for (child in node.children()) {
            val childPath = findNodePath(child, line, col)
            if (childPath.isNotEmpty()) return listOf(node) + childPath
        }
        return listOf(node)
    }

    /**
     * Check if a span contains the given position (line, col).
     * LSP uses 0-based line and column indices.
     */
    private fun contains(span: Span, line: Int, col: Int): Boolean {
        if (line < span.startLine || line > span.endLine) return false
        if (line == span.startLine && col < span.startCol) return false
        if (line == span.endLine && col > span.endCol) return false
        return true
    }

    /**
     * Collect scoped variables based on the cursor's position in the AST.
     *
     * Rules:
     * - Inside a procedure: collect params + locals from this procedure only
     * - Always add global variables (MAKE outside any procedure)
     */
    private fun collectScopedVariables(path: List<Node>, program: Program): Set<String> {
        val variables = mutableSetOf<String>()

        // Find enclosing procedure (if any)
        val enclosingProc = path.filterIsInstance<ProcedureDefinition>().lastOrNull()

        if (enclosingProc != null) {
            // Inside a procedure: collect params + locals from this procedure only
            enclosingProc.params.mapTo(variables) { it.text.removePrefix(":") }
            for (node in enclosingProc.walk()) {
                when (node) {
                    is VariableAssignment -> variables.add(node.nameToken.text.removePrefix("\""))
                    is LocalDeclaration -> node.names.mapTo(variables) { it.text.removePrefix("\"") }
                    is ForStatement -> variables.add(node.varName.text)
                    else -> {}
                }
            }
        }

        // Always add global variables (MAKE outside any procedure)
        for (stmt in program.statements) {
            if (stmt is VariableAssignment && !stmt.local) {
                variables.add(stmt.nameToken.text.removePrefix("\""))
            }
        }

        return variables
    }

    /**
     * Check if a suggestion matches the prefix (case-insensitive).
     */
    private fun matchesPrefix(suggestion: String, prefix: String): Boolean {
        return suggestion.uppercase(Locale.ROOT).startsWith(prefix.uppercase(Locale.ROOT))
    }

    /**
     * Collect all user-defined procedure names from the parse result.
     */
    private fun collectProcedures(program: Program): Set<String> {
        return program.walk()
            .filterIsInstance<ProcedureDefinition>()
            .map { it.nameToken.text }
            .toSet()
    }

    /**
     * Create a completion item for a command.
     */
    private fun commandCompletion(name: String, arity: Int): CompletionItem {
        val item = CompletionItem(name)
        item.kind = CompletionItemKind.Function
        item.detail = "LOGO command (arity: $arity)"
        return item
    }

    /**
     * Create a completion item for a variable.
     */
    private fun variableCompletion(name: String): CompletionItem {
        val item = CompletionItem(name)
        item.kind = CompletionItemKind.Variable
        item.detail = "LOGO variable"
        return item
    }

    /**
     * Create a completion item for a keyword.
     */
    private fun keywordCompletion(name: String): CompletionItem {
        val item = CompletionItem(name)
        item.kind = CompletionItemKind.Keyword
        item.detail = "LOGO keyword"
        return item
    }
}
