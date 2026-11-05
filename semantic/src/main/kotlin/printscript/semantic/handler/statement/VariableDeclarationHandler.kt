package printscript.semantic.handler.statement

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Handler
import printscript.semantic.SemanticResult
import printscript.semantic.StatementValidator

class VariableDeclarationHandler : Handler<Statement, StatementValidator, SemanticResult<Unit>> {
    override fun applies(node: Statement) = node is VariableDeclaration

    override fun handle(
        node: Statement,
        ctx: StatementValidator,
    ): SemanticResult<Unit> {
        if (node !is VariableDeclaration) {
            return SemanticResult.Failure(
                "Unexpected node in VariableDeclarationHandler.",
                node.position,
            )
        }

        validateConstHasValue(node)?.let { return it }
        validateValueType(node, ctx)?.let { return it }

        return ctx.symbolTable.define(node.name, node.type, node.isConst, at = node.namePosition)
    }

    private fun validateConstHasValue(node: VariableDeclaration): SemanticResult.Failure? =
        if (node.isConst && node.value == null) {
            SemanticResult.Failure(
                "Constant '${node.name}' must be initialized.",
                node.position,
            )
        } else {
            null
        }

    private fun validateValueType(
        node: VariableDeclaration,
        ctx: StatementValidator,
    ): SemanticResult.Failure? {
        val value = node.value ?: return null
        return when (val valueType = ctx.expressionResolver.resolveType(value, expectedType = node.type)) {
            is SemanticResult.Failure -> valueType
            is SemanticResult.Success -> {
                if (valueType.value != node.type) {
                    SemanticResult.Failure("Incompatible types.", node.position)
                } else {
                    null
                }
            }
        }
    }
}
