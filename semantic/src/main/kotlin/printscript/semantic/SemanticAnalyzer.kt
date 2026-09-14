package printscript.semantic

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.semantic.symbol.SymbolTable

class SemanticAnalyzer(
    val version: LanguageVersion,
) {
    fun analyze(ast: Iterator<Statement>): Iterator<SemanticResult<Statement>> =
        iterator {
            val rules = SemanticRules.from(version)
            val symbolTable = SymbolTable()
            val expressionResolver = ExpressionResolver(symbolTable, rules)
            val statementValidator = StatementValidator(symbolTable, expressionResolver, rules)

            for (statement in ast) {
                val result = statementValidator.validate(statement)
                when (result) {
                    is SemanticResult.Failure -> {
                        val position = if (result.position == Position(0, 0)) statement.position else result.position
                        yield(SemanticResult.Failure(result.message, position))
                        break
                    }
                    is SemanticResult.Success -> {
                        yield(SemanticResult.Success(statement))
                    }
                }
            }
        }
}
