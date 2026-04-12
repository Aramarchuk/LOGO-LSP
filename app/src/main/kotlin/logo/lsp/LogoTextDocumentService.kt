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

    override fun didOpen(params: DidOpenTextDocumentParams?) {
        if (params == null) return
        documentManager.open(params.textDocument.uri, params.textDocument.text)
    }

    override fun didChange(params: DidChangeTextDocumentParams?) {
        if (params == null) return
        val text = params.contentChanges.firstOrNull()?.text ?: return
        documentManager.update(params.textDocument.uri, text)
    }

    override fun didClose(params: DidCloseTextDocumentParams?) {
        if (params == null) return
        documentManager.close(params.textDocument.uri)
    }

    override fun didSave(params: DidSaveTextDocumentParams?) {}

    override fun semanticTokensFull(params: SemanticTokensParams?): CompletableFuture<SemanticTokens> {
        if (params == null) return CompletableFuture.completedFuture(SemanticTokens(emptyList()))
        val tokens = documentManager.getTokens(params.textDocument.uri) ?: emptyList()
        return CompletableFuture.completedFuture(SemanticTokensProvider.computeTokens(tokens))
    }

    override fun declaration(params: DeclarationParams?): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        if (params == null) return CompletableFuture.completedFuture(Either.forLeft(emptyList()))
        val parseResult = documentManager.getParseResult(params.textDocument.uri)
            ?: return CompletableFuture.completedFuture(Either.forLeft(emptyList()))
        val locations = GoToDeclarationProvider.findDeclaration(params.textDocument.uri, params.position, parseResult)
        return CompletableFuture.completedFuture(Either.forLeft(locations))
    }

    override fun completion(params: CompletionParams?): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        if (params == null) return CompletableFuture.completedFuture(Either.forLeft(emptyList()))
        val parseResult = documentManager.getParseResult(params.textDocument.uri)
            ?: return CompletableFuture.completedFuture(Either.forLeft(emptyList()))
        val completions = CompletionProvider.computeCompletions(params.position, parseResult)
        return CompletableFuture.completedFuture(Either.forLeft(completions))
    }
}
