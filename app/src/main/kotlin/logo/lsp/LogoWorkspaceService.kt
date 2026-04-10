package logo.lsp

import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.services.WorkspaceService

class LogoWorkspaceService : WorkspaceService {

    /**
     * Handle configuration change notifications.
     * Stub: no configuration options are currently supported.
     */
    override fun didChangeConfiguration(params: DidChangeConfigurationParams?) {
        // No-op: LOGO LSP does not support dynamic configuration
    }

    /**
     * Handle watched files change notifications.
     * Stub: file watching is not required for LSP (client handles file sync).
     */
    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) {
        // No-op: using full document sync instead
    }
}
