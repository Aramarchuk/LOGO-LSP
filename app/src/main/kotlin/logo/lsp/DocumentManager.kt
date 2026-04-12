package logo.lsp

import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.PublishDiagnosticsParams
import logo.lexer.Lexer
import logo.lexer.Token
import logo.parser.Parser
import logo.features.DiagnosticsProvider

class DocumentManager {

    private data class DocumentState(
        val text: String,
        val tokens: List<Token>,
        val parseResult: Parser.ParseResult,
    )

    private val documents = mutableMapOf<String, DocumentState>()
    private var languageClient: LanguageClient? = null

    fun connect(client: LanguageClient) {
        this.languageClient = client
    }

    fun open(uri: String, text: String) {
        update(uri, text)
    }

    fun update(uri: String, text: String) {
        val tokens = Lexer(text).tokenize()
        val parseResult = Parser(tokens).parse()
        documents[uri] = DocumentState(text, tokens, parseResult)
        publishDiagnostics(uri, parseResult)
    }

    fun close(uri: String) {
        documents.remove(uri)
        languageClient?.publishDiagnostics(PublishDiagnosticsParams(uri, emptyList()))
    }

    fun getTokens(uri: String): List<Token>? = documents[uri]?.tokens

    fun getParseResult(uri: String): Parser.ParseResult? = documents[uri]?.parseResult

    private fun publishDiagnostics(uri: String, parseResult: Parser.ParseResult) {
        val diagnostics = DiagnosticsProvider.computeDiagnostics(parseResult)
        languageClient?.publishDiagnostics(PublishDiagnosticsParams(uri, diagnostics))
    }
}
