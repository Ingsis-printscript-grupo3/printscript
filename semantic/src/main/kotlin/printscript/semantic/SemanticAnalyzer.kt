package printscript.semantic

import printscript.ast.Statement
import printscript.common.LanguageVersion
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
                        yield(SemanticResult.Failure(result.message, statement.position))
                        break
                    }
                    is SemanticResult.Success -> {
                        yield(SemanticResult.Success(statement))
                    }
                }
            }
        }
}
