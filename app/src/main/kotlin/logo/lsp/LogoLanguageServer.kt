package logo.lsp

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.DeclarationRegistrationOptions
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions
import org.eclipse.lsp4j.SemanticTokensServerFull
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture
import logo.features.SemanticTokensProvider

class LogoLanguageServer : LanguageServer, LanguageClientAware {

    private var languageClient: LanguageClient? = null
    private var documentManager: DocumentManager? = null
    private var textDocumentService: LogoTextDocumentService? = null
    private var workspaceService: LogoWorkspaceService? = null

    /**
     * Initialize the server and declare its capabilities.
     * Declares support for:
     * - Semantic tokens (syntax highlighting)
     * - Declaration (go-to-definition)
     * - Completion (autocompletion)
     * - Full document sync (re-parse on every change)
     */
    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
        return CompletableFuture.supplyAsync {
            documentManager = DocumentManager(languageClient)
            textDocumentService = LogoTextDocumentService(documentManager!!)
            workspaceService = LogoWorkspaceService()

            val capabilities = ServerCapabilities()

            // Text document sync: full document (LOGO files are small)
            capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full)

            // Semantic tokens for syntax highlighting
            val semanticTokensOptions = SemanticTokensWithRegistrationOptions()
            semanticTokensOptions.legend = SemanticTokensProvider.tokenLegend()
            semanticTokensOptions.full = Either.forLeft(true)
            capabilities.semanticTokensProvider = semanticTokensOptions

            // Go-to-declaration
            capabilities.declarationProvider = Either.forRight(DeclarationRegistrationOptions())

            // Completion
            capabilities.completionProvider = CompletionOptions()

            InitializeResult(capabilities)
        }
    }

    /**
     * Shutdown the server.
     */
    override fun shutdown(): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Exit the server process.
     */
    override fun exit() {
        // No-op: LSP client will terminate the process
    }

    /**
     * Get the text document service for handling document-level operations.
     */
    override fun getTextDocumentService(): TextDocumentService {
        return textDocumentService ?: throw IllegalStateException("TextDocumentService not initialized")
    }

    /**
     * Get the workspace service for handling workspace-level operations.
     */
    override fun getWorkspaceService(): WorkspaceService {
        return workspaceService ?: throw IllegalStateException("WorkspaceService not initialized")
    }

    /**
     * Connect to the language client.
     * Store the client reference for sending diagnostics and other notifications later.
     */
    override fun connect(client: LanguageClient?) {
        this.languageClient = client
    }
}
