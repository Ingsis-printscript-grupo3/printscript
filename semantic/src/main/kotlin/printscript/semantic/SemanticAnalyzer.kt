package printscript.semantic

import printscript.ast.Statement
import printscript.ast.registry.Registry
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
        val rules = SemanticRules.from(version)
        val symbolTable = SymbolTable()
        val expressionResolver =
            ExpressionResolver(symbolTable, rules, Registry(ExpressionResolver.defaultHandlers(version)))
        return StatementValidator(
            symbolTable,
            expressionResolver,
            rules,
            Registry(StatementValidator.defaultHandlers(version)),
        )
    }
}
