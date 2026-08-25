package printscript.semantic

import printscript.ast.*
import printscript.semantic.symbol.SymbolTable

class StatementValidator(
    private val symbolTable: SymbolTable,
    private val expressionResolver: ExpressionResolver
) {
    fun validate(statement: Statement): SemanticResult<Unit> {
        when (statement) {
            is VariableDeclaration -> {
                statement.value?.let { value ->
                    val exprResult = expressionResolver.resolveType(value)
                    if (exprResult is SemanticResult.Failure) return exprResult
                    val exprType = (exprResult as SemanticResult.Success).value
                    if (exprType != statement.type) return SemanticResult.Failure("Incompatible types.")
                }
                return symbolTable.define(statement.name, statement.type)
            }
            is Assignment -> {
                val exprResult = expressionResolver.resolveType(statement.value)
                if (exprResult is SemanticResult.Failure) return exprResult
                val exprType = (exprResult as SemanticResult.Success).value
                
                val expectedResult = symbolTable.lookup(statement.name)
                if (expectedResult is SemanticResult.Failure) return expectedResult
                val expectedType = (expectedResult as SemanticResult.Success).value
                
                if (exprType != expectedType) return SemanticResult.Failure("Incompatible types in assignment.")
                return SemanticResult.Success(Unit)
            }
            is PrintCall -> {
                val exprResult = expressionResolver.resolveType(statement.value)
                if (exprResult is SemanticResult.Failure) return exprResult
                return SemanticResult.Success(Unit)
            }
            else -> return SemanticResult.Failure("Unknown statement")
        }
    }
}
