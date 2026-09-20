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

        validateRules(node, ctx)?.let { return it }
        validateValueType(node, ctx)?.let { return it }

        return ctx.symbolTable.define(node.name, node.type, node.isConst, at = node.namePosition)
    }

    private fun validateRules(
        node: VariableDeclaration,
        ctx: StatementValidator,
    ): SemanticResult.Failure? =
        when {
            node.isConst && !ctx.rules.supportsConst ->
                SemanticResult.Failure(
                    "'const' declarations are not supported in PrintScript ${ctx.rules.version.label}.",
                    node.position,
                )
            node.isConst && node.value == null ->
                SemanticResult.Failure(
                    "Constant '${node.name}' must be initialized.",
                    node.position,
                )
            node.type !in ctx.rules.supportedTypes ->
                SemanticResult.Failure(
                    "Type '${node.type}' is not supported in PrintScript ${ctx.rules.version.label}.",
                    node.position,
                )
            else -> null
        }

    private fun validateValueType(
        node: VariableDeclaration,
        ctx: StatementValidator,
    ): SemanticResult.Failure? {
        val value = node.value ?: return null
        return when (val exprResult = ctx.expressionResolver.resolveType(value, expectedType = node.type)) {
            is SemanticResult.Failure -> exprResult
            is SemanticResult.Success -> {
                if (exprResult.value != node.type) {
                    SemanticResult.Failure("Incompatible types.", node.position)
                } else {
                    null
                }
            }
        }
    }
}
