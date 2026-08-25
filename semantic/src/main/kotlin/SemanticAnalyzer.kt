package printscript.semantic

import printscript.ast.Statement
import printscript.semantic.symbol.SymbolTable

class SemanticAnalyzer {
    fun analyze(ast: List<Statement>): List<SemanticResult<Unit>> {
        val symbolTable = SymbolTable()
        val expressionResolver = ExpressionResolver(symbolTable)
        val statementValidator = StatementValidator(symbolTable, expressionResolver)

        val results = mutableListOf<SemanticResult<Unit>>()
        for (statement in ast) {
            val result = statementValidator.validate(statement)
            results.add(result)
            if (result is SemanticResult.Failure) break
        }
        return results
    }
}
