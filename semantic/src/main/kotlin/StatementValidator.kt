package printscript.semantic

import printscript.ast.*
import printscript.semantic.symbol.SymbolTable

class StatementValidator(
    private val symbolTable: SymbolTable,
    private val expressionResolver: ExpressionResolver
) {
    fun validate(statement: Statement) {
        when (statement) {
            is VariableDeclaration -> {
                statement.value?.let { value ->
                    val exprType = expressionResolver.resolveType(value)
                    if (exprType != statement.type) throw RuntimeException("Incompatible types.")
                }
                symbolTable.define(statement.name, statement.type)
            }
            is Assignment -> {
                val exprType = expressionResolver.resolveType(statement.value)
                val expectedType = symbolTable.lookup(statement.name)
                if (exprType != expectedType) throw RuntimeException("Incompatible types in assignment.")
            }
            is PrintCall -> {
                expressionResolver.resolveType(statement.value)
            }
            else -> throw RuntimeException("Unknown statement")
        }
    }
}
