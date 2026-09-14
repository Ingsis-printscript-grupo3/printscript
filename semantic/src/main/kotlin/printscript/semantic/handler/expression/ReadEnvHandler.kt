package printscript.semantic.handler.expression

import printscript.ast.Expression
import printscript.ast.ReadEnv
import printscript.ast.registry.Handler
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult

class ReadEnvHandler : Handler<Expression, ExpressionResolver, SemanticResult<String>> {
    override fun applies(node: Expression) = node is ReadEnv

    override fun handle(
        node: Expression,
        ctx: ExpressionResolver,
    ): SemanticResult<String> {
        if (node !is ReadEnv) {
            return SemanticResult.Failure("Unexpected node in ReadEnvHandler.", node.position)
        }
        val argRes = validateArgument(node.argument, ctx)
        if (argRes is SemanticResult.Failure) return argRes

        return validateResolvedType(ctx, node)
    }

    private fun validateArgument(
        argument: Expression,
        ctx: ExpressionResolver,
    ): SemanticResult<Unit> =
        when (val argResult = ctx.resolveType(argument, expectedType = "string")) {
            is SemanticResult.Failure -> argResult
            is SemanticResult.Success ->
                if (argResult.value != "string") {
                    SemanticResult.Failure(
                        "'readEnv' argument must be a string, found '${argResult.value}'.",
                        argument.position,
                    )
                } else {
                    SemanticResult.Success(Unit)
                }
        }

    private fun validateResolvedType(
        ctx: ExpressionResolver,
        node: ReadEnv,
    ): SemanticResult<String> {
        val resolvedType = ctx.expectedType ?: "string"
        if (resolvedType !in ctx.rules.supportedTypes) {
            return SemanticResult.Failure(
                "Type '$resolvedType' is not supported in PrintScript ${ctx.rules.version.label}.",
                node.position,
            )
        }
        return SemanticResult.Success(resolvedType)
    }
}
