# LOGO Language Server

LSP server for the LOGO programming language, written in Kotlin.

## Status

Work in progress. Currently implemented:

- **Lexer** — tokenizes LOGO source code into a stream of typed tokens (keywords, numbers, variable references, quoted words, operators, comments, etc.)

## Build & Run

Requires JDK 17+. Gradle wrapper is included — no Gradle installation needed.

```bash
# Run tests
./gradlew test

# Build fat JAR
./gradlew shadowJar

# Run the server
java -jar app/build/libs/logo-lsp.jar
```

## Architecture

```
source text → Lexer → tokens → [Parser → AST → Analyzer] → LSP features
```

### Lexer (`logo.lexer`)

Single-pass character scanner that produces a flat list of `Token` objects. Each token carries its type, original text, line/column position, and length.

Key design decisions:
- **Case-insensitive keywords**: the lexer matches words like `TO`, `REPEAT`, `MAKE` against a keyword table (uppercased). Original casing is preserved in the token text.
- **`:varname` is a single token** (type `VARIABLE_REF`) — the colon is part of the token, not a separate operator.
- **`"word` is a single token** (type `QUOTED_WORD`) — LOGO uses a leading `"` with no closing quote. The word ends at the next delimiter.
- **Comments** (`;` to end of line) are preserved as tokens — needed later for semantic highlighting, skipped by the parser.
- **Newlines** are emitted as tokens — the parser uses them as statement separators.
- **`-` is always `MINUS`** — the parser will decide whether it's unary negation or binary subtraction based on context.

### Project layout

```
app/src/main/kotlin/logo/
├── Main.kt                  # Entry point
└── lexer/
    ├── Token.kt             # Token data class + TokenType enum
    └── Lexer.kt             # Tokenizer
```
