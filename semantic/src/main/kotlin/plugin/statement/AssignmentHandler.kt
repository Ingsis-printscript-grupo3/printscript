package printscript.semantic.plugin.statement

import printscript.ast.Assignment
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.plugin.StatementHandler
import printscript.semantic.symbol.SymbolTable

class AssignmentHandler : StatementHandler<Assignment>(Assignment::class.java) {
    override fun doValidate(
        statement: Assignment,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<Unit> {
        val exprResult = resolver.resolveType(statement.value)
        if (exprResult is SemanticResult.Failure) return exprResult
        val exprType = (exprResult as SemanticResult.Success).value

        val expectedResult = symbolTable.lookup(statement.name)
        if (expectedResult is SemanticResult.Failure) return expectedResult
        val expectedType = (expectedResult as SemanticResult.Success).value

        if (exprType != expectedType) return SemanticResult.Failure("Incompatible types in assignment.")
        return SemanticResult.Success(Unit)
    }
}
