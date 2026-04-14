package logo.features

import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.Position
import logo.parser.CommandCall
import logo.parser.ForStatement
import logo.parser.LocalDeclaration
import logo.parser.Node
import logo.parser.Parser
import logo.parser.ProcedureDefinition
import logo.parser.Program
import logo.parser.VariableAssignment
import logo.parser.VariableRef
import logo.parser.WordLiteral
import logo.parser.children
import logo.parser.findNodePath
import logo.parser.walk
import logo.analysis.BuiltinCommands
import java.util.Locale

object CompletionProvider {

    private val KEYWORDS = logo.lexer.LogoKeywords.Keywords

    fun computeCompletions(position: Position, parseResult: Parser.ParseResult): List<CompletionItem> {
        val program = parseResult.program
        val path = program.findNodePath(position.line, position.character)
        val deepestNode = path.lastOrNull()

        if (deepestNode is VariableRef) {
            val prefix = deepestNode.token.text.removePrefix(":")
            return variableCompletions(path, prefix)
        }

        val prefix = when (deepestNode) {
            is CommandCall -> deepestNode.nameToken.text
            is WordLiteral -> deepestNode.token.text
            else -> ""
        }
        return commandCompletions(program, prefix)
    }

    // -- Completion by context --

    private fun variableCompletions(path: List<Node>, prefix: String): List<CompletionItem> {
        return collectScopedVariables(path)
            .filter { matchesPrefix(it, prefix) }
            .map { variableCompletion(it) }
    }

    private fun commandCompletions(program: Program, prefix: String): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()

        for (cmd in BuiltinCommands.all()) {
            if (matchesPrefix(cmd.name, prefix)) {
                completions += commandCompletion(cmd.name, cmd.arity)
            }
        }
        for ((name, arity) in collectProcedures(program)) {
            if (matchesPrefix(name, prefix)) {
                completions += commandCompletion(name, arity)
            }
        }
        for (keyword in KEYWORDS) {
            if (matchesPrefix(keyword, prefix)) {
                completions += keywordCompletion(keyword)
            }
        }

        return completions
    }

    // -- Scope analysis --

    private fun collectScopedVariables(path: List<Node>): Set<String> {
        val variables = mutableSetOf<String>()
        for (node in path) {
            when (node) {
                is Program -> walkSkippingProcedures(node, variables)
                is ProcedureDefinition -> {
                    node.params.mapTo(variables) { it.text.removePrefix(":") }
                    for (stmt in node.body) {
                        walkSkippingProcedures(stmt, variables)
                    }
                }
                else -> {}
            }
        }
        return variables
    }

    private fun walkSkippingProcedures(node: Node, variables: MutableSet<String>) {
        when (node) {
            is ProcedureDefinition -> return
            is VariableAssignment -> variables += node.nameToken.text.removePrefix("\"")
            is LocalDeclaration -> node.names.mapTo(variables) { it.text.removePrefix("\"") }
            is ForStatement -> variables += node.varName.text
            else -> {}
        }
        for (child in node.children()) {
            walkSkippingProcedures(child, variables)
        }
    }

    private fun collectProcedures(program: Program): List<Pair<String, Int>> {
        return program.walk()
            .filterIsInstance<ProcedureDefinition>()
            .map { it.nameToken.text to it.params.size }
            .toList()
    }

    // -- CompletionItem builders --

    private fun matchesPrefix(suggestion: String, prefix: String): Boolean =
        suggestion.uppercase(Locale.ROOT).startsWith(prefix.uppercase(Locale.ROOT))

    private fun commandCompletion(name: String, arity: Int): CompletionItem {
        val item = CompletionItem(name)
        item.kind = CompletionItemKind.Function
        item.detail = "LOGO command (arity: $arity)"
        return item
    }

    private fun variableCompletion(name: String): CompletionItem {
        val item = CompletionItem(name)
        item.kind = CompletionItemKind.Variable
        item.detail = "LOGO variable"
        return item
    }

    private fun keywordCompletion(name: String): CompletionItem {
        val item = CompletionItem(name)
        item.kind = CompletionItemKind.Keyword
        item.detail = "LOGO keyword"
        return item
    }
}
