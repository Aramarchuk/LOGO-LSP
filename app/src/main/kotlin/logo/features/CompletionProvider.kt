package logo.features

import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.Position
import logo.parser.CommandCall
import logo.parser.ErrorNode
import logo.parser.ForStatement
import logo.parser.ForEachStatement
import logo.parser.ForeverStatement
import logo.parser.IfStatement
import logo.parser.LocalDeclaration
import logo.parser.Node
import logo.parser.NumberLiteral
import logo.parser.OutputStatement
import logo.parser.ParenExpr
import logo.parser.Parser
import logo.parser.ProcedureDefinition
import logo.parser.Program
import logo.parser.RepeatStatement
import logo.parser.StopStatement
import logo.parser.UnaryExpr
import logo.parser.VariableAssignment
import logo.parser.VariableRef
import logo.parser.WhileStatement
import logo.parser.WordLiteral
import logo.parser.BooleanLiteral
import logo.parser.BinaryExpr
import logo.parser.ListLiteral
import logo.parser.children
import logo.parser.findNodePath
import logo.parser.walk
import logo.analysis.BuiltinCommands
import java.util.Locale

object CompletionProvider {

    private val KEYWORDS = logo.lexer.LogoKeywords.keywords

    private enum class CompletionKind {
        VARIABLE,
        COMMAND,
        NONE,
    }

    private data class CompletionContext(
        val kind: CompletionKind,
        val prefix: String = "",
        val path: List<Node> = emptyList(),
    )

    fun computeCompletions(position: Position, parseResult: Parser.ParseResult): List<CompletionItem> {
        val program = parseResult.program
        val path = program.findNodePath(position.line, position.character)
        val deepestNode = path.lastOrNull()
        val context = resolveContext(path, deepestNode)

        return when (context.kind) {
            CompletionKind.VARIABLE -> variableCompletions(context.path, context.prefix)
            CompletionKind.COMMAND -> commandCompletions(program, context.prefix)
            CompletionKind.NONE -> emptyList()
        }
    }

    private fun resolveContext(path: List<Node>, deepestNode: Node?): CompletionContext {
        if (deepestNode is VariableRef) {
            return CompletionContext(
                kind = CompletionKind.VARIABLE,
                prefix = deepestNode.token.text.removePrefix(":"),
                path = path,
            )
        }
        if (deepestNode is CommandCall) {
            return CompletionContext(
                kind = CompletionKind.COMMAND,
                prefix = deepestNode.nameToken.text,
            )
        }
        if (isStatementCommandContext(deepestNode)) {
            return CompletionContext(kind = CompletionKind.COMMAND)
        }
        return CompletionContext(kind = CompletionKind.NONE)
    }

    private fun isStatementCommandContext(node: Node?): Boolean {
        if (node == null) return true

        if (node is NumberLiteral ||
            node is WordLiteral ||
            node is BooleanLiteral ||
            node is BinaryExpr ||
            node is UnaryExpr ||
            node is ParenExpr
        ) {
            return false
        }

        return node is Program ||
            node is ProcedureDefinition ||
            node is ListLiteral ||
            node is RepeatStatement ||
            node is IfStatement ||
            node is WhileStatement ||
            node is ForStatement ||
            node is ForEachStatement ||
            node is ForeverStatement ||
            node is VariableAssignment ||
            node is LocalDeclaration ||
            node is OutputStatement ||
            node is StopStatement ||
            node is ErrorNode
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
