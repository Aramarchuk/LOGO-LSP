package logo.lsp

import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.PublishDiagnosticsParams
import logo.lexer.Lexer
import logo.lexer.Token
import logo.parser.Parser
import logo.features.DiagnosticsProvider

/**
 * Manages document state: text, tokens, and parse results.
 * Handles lexing, parsing, and diagnostic publishing.
 */
class DocumentManager(private val languageClient: LanguageClient?) {

    /**
     * State of a single document.
     */
    private data class DocumentState(
        val uri: String,
        val text: String,
        val tokens: List<Token>,
        val parseResult: Parser.ParseResult,
    )

    private val documents = mutableMapOf<String, DocumentState>()

    /**
     * Open a new document or replace existing one.
     * Lex and parse the text, then publish diagnostics.
     */
    fun open(uri: String, text: String) {
        update(uri, text)
    }

    /**
     * Update document content (full document sync).
     * Lex and parse the text, then publish diagnostics.
     */
    fun update(uri: String, text: String) {
        // Lex the document
        val lexer = Lexer(text)
        val tokens = lexer.tokenize()

        // Parse the document
        val parser = Parser(tokens)
        val parseResult = parser.parse()

        // Store document state
        documents[uri] = DocumentState(uri, text, tokens, parseResult)

        // Publish diagnostics
        publishDiagnostics(uri, parseResult)
    }

    /**
     * Close a document (remove from cache).
     */
    fun close(uri: String) {
        documents.remove(uri)
        // Clear diagnostics for this document
        languageClient?.publishDiagnostics(PublishDiagnosticsParams(uri, emptyList()))
    }

    /**
     * Get the tokens for a document.
     */
    fun getTokens(uri: String): List<Token>? {
        return documents[uri]?.tokens
    }

    /**
     * Get the parse result for a document.
     */
    fun getParseResult(uri: String): Parser.ParseResult? {
        return documents[uri]?.parseResult
    }

    /**
     * Get the source text for a document.
     */
    fun getText(uri: String): String? {
        return documents[uri]?.text
    }

    /**
     * Publish diagnostics for a document.
     */
    private fun publishDiagnostics(uri: String, parseResult: Parser.ParseResult) {
        val diagnostics = DiagnosticsProvider.computeDiagnostics(parseResult)
        languageClient?.publishDiagnostics(PublishDiagnosticsParams(uri, diagnostics))
    }
}
