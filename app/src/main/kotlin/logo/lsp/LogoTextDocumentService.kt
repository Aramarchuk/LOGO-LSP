package logo.lsp

import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.DeclarationParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture
import logo.features.SemanticTokensProvider
import logo.features.GoToDeclarationProvider
import logo.features.CompletionProvider

class LogoTextDocumentService(private val documentManager: DocumentManager) : TextDocumentService {

    /**
     * Handle document open: lex, parse, and store the document.
     */
    override fun didOpen(params: DidOpenTextDocumentParams?) {
        if (params == null) return
        val uri = params.textDocument.uri
        val text = params.textDocument.text
        documentManager.open(uri, text)
    }

    /**
     * Handle document change: re-lex and re-parse (full document sync).
     */
    override fun didChange(params: DidChangeTextDocumentParams?) {
        if (params == null) return
        val uri = params.textDocument.uri
        // In full document sync, the change contains the entire new document
        val text = params.contentChanges.firstOrNull()?.text ?: return
        documentManager.update(uri, text)
    }

    /**
     * Handle document close: remove from storage.
     */
    override fun didClose(params: DidCloseTextDocumentParams?) {
        if (params == null) return
        val uri = params.textDocument.uri
        documentManager.close(uri)
    }

    /**
     * Handle document save: no-op (we use full document sync).
     */
    override fun didSave(params: DidSaveTextDocumentParams?) {
        // No-op: document is already synced via didChange
    }

    /**
     * Provide semantic tokens (syntax highlighting) for the entire document.
     * Maps lexer tokens to LSP semantic token types.
     */
    override fun semanticTokensFull(params: SemanticTokensParams?): CompletableFuture<SemanticTokens> {
        return CompletableFuture.supplyAsync {
            if (params == null) return@supplyAsync SemanticTokens(emptyList())

            val uri = params.textDocument.uri
            val tokens = documentManager.getTokens(uri) ?: emptyList()
            SemanticTokensProvider.computeTokens(tokens)
        }
    }

    /**
     * Go to declaration: find the definition of a symbol at the given position.
     * Resolves variables (:varname) and procedures (TO name) to their definition locations.
     */
    override fun declaration(params: DeclarationParams?): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        return CompletableFuture.supplyAsync {
            if (params == null) return@supplyAsync Either.forLeft(emptyList())

            val uri = params.textDocument.uri
            val position = params.position
            val parseResult = documentManager.getParseResult(uri) ?: return@supplyAsync Either.forLeft(emptyList())

            val locations = GoToDeclarationProvider.findDeclaration(uri, position, parseResult)
            Either.forLeft(locations)
        }
    }

    /**
     * Provide completions at the given position.
     * Variable reference context → variable names
     * Command/keyword context → builtin commands + user procedures + keywords
     */
    override fun completion(params: CompletionParams?): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        return CompletableFuture.supplyAsync {
            if (params == null) return@supplyAsync Either.forLeft(emptyList())

            val uri = params.textDocument.uri
            val position = params.position
            val parseResult = documentManager.getParseResult(uri) ?: return@supplyAsync Either.forLeft(emptyList())

            val completions = CompletionProvider.computeCompletions(position, parseResult)
            Either.forLeft(completions)
        }
    }
}
