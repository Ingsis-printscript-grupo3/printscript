package printscript.semantic.handler.statement

import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.semantic.SemanticResult
import printscript.semantic.StatementValidator

class PrintCallHandler : Handler<Statement, StatementValidator, SemanticResult<Unit>> {
    override fun applies(node: Statement) = node is PrintCall

    override fun handle(
        node: Statement,
        ctx: StatementValidator,
    ): SemanticResult<Unit> {
        if (node !is PrintCall) {
            return SemanticResult.Failure("Unexpected node in PrintCallHandler.", node.position)
        }

        return when (val exprResult = ctx.expressionResolver.resolveType(node.value, expectedType = "string")) {
            is SemanticResult.Failure -> exprResult
            is SemanticResult.Success -> SemanticResult.Success(Unit)
        }
    }
}
