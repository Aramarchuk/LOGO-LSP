package logo.features

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import logo.parser.Parser

object DiagnosticsProvider {

    /**
     * Compute diagnostics (errors and warnings) from parse results.
     * Converts ParseError objects to LSP Diagnostic format.
     *
     * Currently reports:
     * - Parse errors (syntax errors)
     *
     * Future:
     * - Analyzer diagnostics (unknown procedures, wrong argument counts, etc.)
     */
    fun computeDiagnostics(parseResult: Parser.ParseResult): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()

        // Convert parse errors
        for (error in parseResult.errors) {
            val diagnostic = Diagnostic(
                Range(
                    Position(error.token.line, error.token.column),
                    Position(error.token.line, error.token.column + error.token.length)
                ),
                error.message,
                DiagnosticSeverity.Error,
                "logo-lsp"
            )
            diagnostics.add(diagnostic)
        }

        // TODO: Add analyzer diagnostics (semantic errors/warnings)
        // - Unknown procedure calls
        // - Wrong argument counts
        // - Undefined variables
        // - etc.

        return diagnostics
    }
}
