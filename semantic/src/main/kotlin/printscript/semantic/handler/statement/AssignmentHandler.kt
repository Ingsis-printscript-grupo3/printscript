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

        val variableResult = ctx.symbolTable.lookupVariable(node.name)
        if (variableResult is SemanticResult.Failure) {
            return variableResult
        }

        val variable = (variableResult as SemanticResult.Success).value
        if (variable.isConst) {
            return SemanticResult.Failure(
                "Semantic Error: Cannot reassign constant '${node.name}'.",
                node.position,
            )
        }

        val exprResult = ctx.expressionResolver.resolveType(node.value, expectedType = variable.type)
        if (exprResult is SemanticResult.Failure) {
            return exprResult
        }

        val exprType = (exprResult as SemanticResult.Success).value
        if (exprType != variable.type) {
            return SemanticResult.Failure("Incompatible types in assignment.")
        }

        return SemanticResult.Success(Unit)
    }
}
