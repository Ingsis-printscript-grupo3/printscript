package printscript.semantic.plugin.statement

import printscript.ast.PrintCall
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.plugin.StatementHandler
import printscript.semantic.symbol.SymbolTable

class PrintCallHandler : StatementHandler<PrintCall>(PrintCall::class.java) {
    override fun doValidate(
        statement: PrintCall,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<Unit> {
        val exprResult = resolver.resolveType(statement.value)
        if (exprResult is SemanticResult.Failure) return exprResult
        return SemanticResult.Success(Unit)
    }
}
