package printscript.semantic

import printscript.ast.*
import printscript.semantic.symbol.SymbolTable

class SemanticAnalyzer {
    fun analyze(ast: Iterator<Statement>): Iterator<SemanticResult<Statement>> = iterator {
        val symbolTable = SymbolTable()
        val expressionResolver = ExpressionResolver(symbolTable)
        val statementValidator = StatementValidator(symbolTable, expressionResolver)

        for (statement in ast) {
            val result = statementValidator.validate(statement)
            when (result) {
                is SemanticResult.Failure -> {
                    yield(SemanticResult.Failure(result.message))
                    break
                }
                is SemanticResult.Success -> {
                    yield(SemanticResult.Success(statement))
                }
            }
        }
    }
}
