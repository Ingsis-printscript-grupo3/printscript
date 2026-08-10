package printscript.semantic

import printscript.ast.*
import printscript.semantic.symbol.SymbolTable

class SemanticAnalyzer {
    fun analyze(ast: List<Statement>) {
        val symbolTable = SymbolTable()
        val expressionResolver = ExpressionResolver(symbolTable)
        val statementValidator = StatementValidator(symbolTable, expressionResolver)

        ast.forEach { statementValidator.validate(it) }
    }
}
