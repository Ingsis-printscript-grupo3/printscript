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
            return SemanticResult.Failure("Unexpected node in AssignmentHandler.", node.position)
        }

        val variable =
            when (val variableResult = ctx.symbolTable.lookup(node.name)) {
                is SemanticResult.Failure -> return SemanticResult.Failure(variableResult.message, node.position)
                is SemanticResult.Success -> variableResult.value
            }

        if (variable.isConst) {
            return SemanticResult.Failure(
                "Cannot reassign constant '${node.name}'.",
                node.position,
            )
        }

        when (val exprResult = ctx.expressionResolver.resolveType(node.value, expectedType = variable.type)) {
            is SemanticResult.Failure -> return exprResult
            is SemanticResult.Success -> {
                if (exprResult.value != variable.type) {
                    return SemanticResult.Failure(
                        "Incompatible types in assignment.",
                        node.position,
                    )
                }
            }
        }

        return SemanticResult.Success(Unit)
    }
}
