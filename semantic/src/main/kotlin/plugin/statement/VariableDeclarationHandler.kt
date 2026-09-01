package printscript.semantic.plugin.statement

import printscript.ast.VariableDeclaration
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.plugin.StatementHandler
import printscript.semantic.symbol.SymbolTable

class VariableDeclarationHandler : StatementHandler<VariableDeclaration>(VariableDeclaration::class.java) {
    override fun doValidate(
        statement: VariableDeclaration,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<Unit> {
        statement.value?.let { value ->
            val exprResult = resolver.resolveType(value)
            if (exprResult is SemanticResult.Failure) return exprResult
            val exprType = (exprResult as SemanticResult.Success).value
            if (exprType != statement.type) return SemanticResult.Failure("Incompatible types.")
        }
        return symbolTable.define(statement.name, statement.type)
    }
}
