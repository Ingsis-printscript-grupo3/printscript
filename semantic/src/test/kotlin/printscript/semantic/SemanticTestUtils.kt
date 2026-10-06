package printscript.semantic

import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.semantic.symbol.SymbolTable

// arman resolver y validator con los handlers de la version, igual que SemanticAnalyzer
fun resolverFor(
    symbolTable: SymbolTable = SymbolTable(),
    version: LanguageVersion = LanguageVersion.V1_1,
): ExpressionResolver =
    ExpressionResolver(symbolTable, SemanticRules.from(version), Registry(ExpressionResolver.defaultHandlers(version)))

fun validatorFor(
    symbolTable: SymbolTable = SymbolTable(),
    version: LanguageVersion = LanguageVersion.V1_1,
): StatementValidator =
    StatementValidator(
        symbolTable,
        resolverFor(symbolTable, version),
        SemanticRules.from(version),
        Registry(StatementValidator.defaultHandlers(version)),
    )
