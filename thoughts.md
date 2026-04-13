1. To build some map or tree with keywords, limit scope.
2. Refactor collectScopedVariables, considering LOCAL keyword.
3. findVariableInProcedure can traverse the procedure body multiple times (findLocalDeclaration walks the full tree, and
4. if that returns null findMakeInProcedure walks it again). Since go-to-declaration may
be invoked often, consider a single walk that checks LOCAL/LOCALMAKE/MAKE in one pass (preserving precedence)
to avoid repeated AST traversal on large procedures.
5. Seems like end of the line also ends all brackets and parentheses.
6. ":" itself doesn't really start any suggestion - that's not cool.
7. I've completely forgotten to add a "%" operator
8. collectScopedVariables still collects all variables in FOR loops
and works inconsistent with MAKE.
9. Numbers still give me every possible command as suggestion.