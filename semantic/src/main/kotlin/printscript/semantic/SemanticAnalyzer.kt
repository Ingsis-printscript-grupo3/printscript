package printscript.semantic

import printscript.ast.Statement
import printscript.semantic.symbol.SymbolTable

// recibe los handlers ya elegidos: que nodos acepta lo decide quien lo arma
class SemanticAnalyzer(
    private val statementHandlers: List<SemanticStatementHandler>,
    private val expressionHandlers: List<SemanticExpressionHandler>,
) {
    fun analyze(ast: Iterator<Statement>): Iterator<SemanticResult<Statement>> =
        iterator {
            val statementValidator = newValidator()

            for (statement in ast) {
                val result = statementValidator.validate(statement)
                when (result) {
                    is SemanticResult.Failure -> {
                        // el error ya viene con su posicion: la pone quien lo crea
                        yield(result)
                        break
                    }
                    is SemanticResult.Success -> {
                        yield(SemanticResult.Success(statement))
                    }
                }
            }
        }

    // uno nuevo por analisis: la tabla de simbolos no se comparte entre corridas
    private fun newValidator(): StatementValidator {
        val symbolTable = SymbolTable()
        val expressionResolver = ExpressionResolver(symbolTable, expressionHandlers)
        return StatementValidator(symbolTable, expressionResolver, statementHandlers)
    }
}
