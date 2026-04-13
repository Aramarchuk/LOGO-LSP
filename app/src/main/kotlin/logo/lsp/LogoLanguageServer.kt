package logo.lsp

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.DeclarationRegistrationOptions
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture
import logo.features.SemanticTokensProvider

class LogoLanguageServer : LanguageServer, LanguageClientAware {

    private val documentManager = DocumentManager()
    private val textDocumentService = LogoTextDocumentService(documentManager)
    private val workspaceService = LogoWorkspaceService()

    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
        val capabilities = ServerCapabilities()

        capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full)

        val semanticTokensOptions = SemanticTokensWithRegistrationOptions()
        semanticTokensOptions.legend = SemanticTokensProvider.tokenLegend()
        semanticTokensOptions.full = Either.forLeft(true)
        capabilities.semanticTokensProvider = semanticTokensOptions

        capabilities.declarationProvider = Either.forRight(DeclarationRegistrationOptions())
        capabilities.completionProvider = CompletionOptions(false, listOf(":"))

        return CompletableFuture.completedFuture(InitializeResult(capabilities))
    }

    override fun shutdown(): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(null)
    }

    override fun exit() {}

    override fun getTextDocumentService(): TextDocumentService = textDocumentService

    override fun getWorkspaceService(): WorkspaceService = workspaceService

    override fun connect(client: LanguageClient?) {
        if (client != null) {
            documentManager.connect(client)
        }
    }
}
