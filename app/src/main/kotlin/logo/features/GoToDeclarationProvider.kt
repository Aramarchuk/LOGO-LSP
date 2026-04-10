package logo.features

import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import logo.parser.Parser

object GoToDeclarationProvider {

    /**
     * Find the declaration location for a symbol at the given position.
     *
     * Resolves:
     * - :varname → finds MAKE "varname or procedure parameter definition
     * - procedureName → finds TO definition
     *
     * Returns a list of Location objects (usually 0 or 1 item).
     */
    fun findDeclaration(uri: String, position: Position, parseResult: Parser.ParseResult): List<Location> {
        // TODO: Implement symbol resolution
        // 1. Find token at position
        // 2. If variable reference, find MAKE or parameter definition
        // 3. If word, find TO definition
        // 4. Build Location from token information
        return emptyList()
    }

    /**
     * Create a Location object from a token and document URI.
     */
    private fun tokenToLocation(uri: String, tokenName: String, line: Int, column: Int): Location {
        val range = Range(
            Position(line, column),
            Position(line, column + tokenName.length)
        )
        return Location(uri, range)
    }
}
