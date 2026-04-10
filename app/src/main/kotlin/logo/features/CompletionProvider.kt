package logo.features

import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.Position
import logo.lexer.Token
import logo.parser.Parser

object CompletionProvider {

    /**
     * Compute completion items at the given position.
     *
     * Context-aware completions:
     * - After ':' → variable names from parse result
     * - Otherwise → builtin commands + user procedures + keywords
     *
     * Returns a list of CompletionItem objects.
     */
    fun computeCompletions(position: Position, tokens: List<Token>, parseResult: Parser.ParseResult): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()

        // TODO: Implement context-aware completions
        // 1. Find token at or before position
        // 2. Determine context:
        //    - If previous token is ':', suggest variable names
        //    - If at statement position, suggest commands
        //    - If in argument position, suggest appropriate values
        // 3. Filter based on partial word before cursor
        // 4. Build CompletionItem list

        // Placeholder: return empty list
        return completions
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
