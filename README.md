# LOGO-LSP

LSP server for the LOGO programming language, written in Kotlin.

## Features

- **Syntax highlighting** — semantic tokens for keywords, commands, variables, numbers, strings, comments, operators
- **Go-to-declaration** — Ctrl+Click on `:varname` jumps to MAKE/LOCAL/parameter definition; on procedure name jumps to TO definition
- **Diagnostics** — parse errors shown as red squiggles
- **Completion** — context-aware: `:` triggers variable names (scoped), otherwise suggests builtin commands, user procedures, keywords

## Build & run

Requires JDK 17+.

```bash
./gradlew test          # run tests (118 cases)
./gradlew shadowJar     # build fat JAR
java -jar app/build/libs/logo-lsp.jar   # start LSP server (stdio)
```

## Connect to an LSP client

### IntelliJ (LSP4IJ plugin)

1. Install the [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) plugin
2. Settings → Languages & Frameworks → Language Servers → Add
3. Command: `java -jar /path/to/logo-lsp.jar`
4. File association: `*.logo`

## Architecture

```
Source text → Lexer → Tokens → Parser → AST (with spans) → LSP features
```

- **Full document sync** — re-lex and re-parse on every edit (LOGO files are small)
- **Two-pass parsing** — Pass 1 scans procedure arities, Pass 2 parses with known arities (needed for arity-dependent parsing)
- **AST with spans** — every node tracks source position for cursor-to-node mapping
- **Case-insensitive** — all lookups normalized to uppercase

## Project layout

```
app/src/main/kotlin/logo/
├── Main.kt                        # LSP stdio launcher
├── lexer/
│   ├── Token.kt                   # Token types + Token data class
│   └── Lexer.kt                   # Single-pass scanner
├── parser/
│   ├── Ast.kt                     # AST nodes + walk() + findNodePath()
│   └── Parser.kt                  # Two-pass recursive descent
├── analysis/
│   └── BuiltinCommands.kt         # ~60 builtin commands with arities
├── lsp/
│   ├── LogoLanguageServer.kt      # LSP entry point, capabilities
│   ├── LogoTextDocumentService.kt # Document events + feature dispatch
│   ├── LogoWorkspaceService.kt    # No-op
│   └── DocumentManager.kt         # Document state + reparse pipeline
└── features/
    ├── SemanticTokensProvider.kt   # Tokens → delta-encoded semantic tokens
    ├── GoToDeclarationProvider.kt  # Cursor position → definition location
    ├── DiagnosticsProvider.kt      # Parse errors → LSP diagnostics
    └── CompletionProvider.kt       # Scoped variable + command completion
```
