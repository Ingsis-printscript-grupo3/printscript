package printscript.semantic.handler.statement

import printscript.ast.Assignment
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.semantic.SemanticResult
import printscript.semantic.StatementValidator

class AssignmentHandler : Handler<Statement, StatementValidator, SemanticResult<Unit>> {
    override fun applies(node: Statement) = node is Assignment

    override fun handle(
        node: Statement,
        ctx: StatementValidator,
    ): SemanticResult<Unit> {
        if (node !is Assignment) {
            return SemanticResult.Failure("Semantic Error: Unexpected node in AssignmentHandler.", node.position)
        }

        val exprResult = ctx.expressionResolver.resolveType(node.value)
        val expectedResult = ctx.symbolTable.lookupType(node.name)

        return when {
            exprResult is SemanticResult.Failure -> exprResult
            expectedResult is SemanticResult.Failure -> expectedResult
            (exprResult as SemanticResult.Success).value != (expectedResult as SemanticResult.Success).value ->
                SemanticResult.Failure("Incompatible types in assignment.")
            else -> SemanticResult.Success(Unit)
        }
    }
}
