package printscript.semantic

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.semantic.symbol.SymbolTable

class SemanticAnalyzer(
    val version: LanguageVersion,
) {
    fun analyze(ast: Iterator<Statement>): Iterator<SemanticResult<Statement>> =
        iterator {
            val symbolTable = SymbolTable()
            val expressionResolver = ExpressionResolver(symbolTable, version)
            val statementValidator = StatementValidator(symbolTable, expressionResolver, version)

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
}
