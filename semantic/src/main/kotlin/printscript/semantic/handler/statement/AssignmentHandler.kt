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
            when (val lookup = ctx.symbolTable.lookup(node.name, at = node.position)) {
                is SemanticResult.Failure -> return lookup
                is SemanticResult.Success -> lookup.value
            }
        if (variable.isConst) {
            return SemanticResult.Failure("Cannot reassign constant '${node.name}'.", node.position)
        }
        return validateAssignmentType(node, ctx, variable.type)
    }

    private fun validateAssignmentType(
        node: Assignment,
        ctx: StatementValidator,
        expectedType: String,
    ): SemanticResult<Unit> =
        when (val valueType = ctx.expressionResolver.resolveType(node.value, expectedType = expectedType)) {
            is SemanticResult.Failure -> valueType
            is SemanticResult.Success ->
                if (valueType.value != expectedType) {
                    SemanticResult.Failure("Incompatible types in assignment.", node.position)
                } else {
                    SemanticResult.Success(Unit)
                }
        }
}
