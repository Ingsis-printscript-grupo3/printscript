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
            return SemanticResult.Failure(
                "Unexpected node in ReadEnvHandler.",
                node.position,
            )
        }

        when (val argResult = ctx.resolveType(node.argument, expectedType = "string")) {
            is SemanticResult.Failure -> return argResult
            is SemanticResult.Success -> {
                if (argResult.value != "string") {
                    return SemanticResult.Failure(
                        "'readEnv' argument must be a string, found '${argResult.value}'.",
                        node.argument.position,
                    )
                }
            }
        }

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
