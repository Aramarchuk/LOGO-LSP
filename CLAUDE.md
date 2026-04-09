# LOGO-LSP

LSP server for the LOGO programming language, written in Kotlin. Test assignment.

## What it does

Implements the Language Server Protocol for LOGO — an educational language with turtle graphics. The server provides:
1. **Semantic tokens** — syntax highlighting (keywords, commands, variables, numbers, comments)
2. **Go-to-declaration** — navigate to procedure and variable definitions
3. **Diagnostics** — errors and warnings (unknown procedures, wrong argument count, etc.)
4. **Completion** — context-aware autocompletion for commands and variables

## Architecture

```
Source text → Lexer → Tokens → Parser → AST → Analyzer → LSP features
```

- **Full document sync** — LOGO files are small; re-parse everything on each edit
- **Two-pass parsing** — Pass 1: scan procedure arities; Pass 2: full parse (needed because LOGO parsing depends on arity)
- **Case-insensitive** — all lookups use uppercase, original casing preserved for display

## Project structure

```
app/src/main/kotlin/logo/
├── Main.kt                      # LSP stdio launcher
├── lexer/
│   ├── Token.kt                 # Token data class + TokenType enum (42+ types)
│   └── Lexer.kt                 # Single-pass character scanner
├── parser/                      # [TODO]
│   ├── Ast.kt                   # Sealed AST hierarchy
│   └── Parser.kt                # Recursive descent, two-pass
├── analysis/                    # [TODO]
│   ├── SymbolTable.kt           # Scoped symbol table
│   ├── Analyzer.kt              # Semantic analysis + diagnostics
│   └── BuiltinCommands.kt       # ~50 built-in commands with arities
├── lsp/                         # [TODO]
│   ├── LogoLanguageServer.kt
│   ├── LogoTextDocumentService.kt
│   ├── LogoWorkspaceService.kt
│   └── DocumentManager.kt
└── features/                    # [TODO]
    ├── SemanticTokensProvider.kt
    ├── GoToDeclarationProvider.kt
    ├── DiagnosticsProvider.kt
    └── CompletionProvider.kt
```

## Build & run

```bash
./gradlew test          # run tests
./gradlew shadowJar     # fat JAR → app/build/libs/logo-lsp.jar
java -jar app/build/libs/logo-lsp.jar   # start server (stdio)
```

## Tech stack

- Kotlin 2.1.20, JVM 17+
- LSP4J 0.24.0
- Gradle + Shadow plugin 8.1.1
- JUnit 5

## Implementation status

- [x] Project skeleton & build system
- [x] Lexer (Token.kt, Lexer.kt) + tests (30+ cases)
- [ ] Built-in Commands Registry
- [ ] Parser (AST + recursive descent)
- [ ] Symbol Table & Analyzer
- [ ] LSP Core (DocumentManager, Server, Services)
- [ ] LSP Features (SemanticTokens, GoToDeclaration, Diagnostics, Completion)

## LOGO language specifics

- Procedures: `TO name :param1 :param2 ... END`
- Variables: `:varname` (reference), `MAKE "varname value` (assignment)
- Case-insensitive keywords
- Control structures: REPEAT, IF, IFELSE, WHILE, FOR, FOREACH, FOREVER
- Comments: `;` to end of line
- Quoted words: `"word` (string literals with no closing quote)
