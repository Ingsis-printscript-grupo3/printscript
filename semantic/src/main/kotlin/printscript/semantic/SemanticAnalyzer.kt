package printscript.semantic

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.semantic.symbol.SymbolTable

// arma resolver y validator con los handlers de su version: es el unico lugar del modulo que la mira
class SemanticAnalyzer(
    val version: LanguageVersion,
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
        val expressionResolver = ExpressionResolver(symbolTable, version)
        return StatementValidator(symbolTable, expressionResolver, version)
    }
}
