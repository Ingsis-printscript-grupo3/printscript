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
                "Semantic Error: Unexpected node in ReadEnvHandler.",
                node.position,
            )
        }

        if (!ctx.rules.allowsReadEnv) {
            return SemanticResult.Failure(
                "Semantic Error: 'readEnv' is not supported in PrintScript ${ctx.rules.version.label}.",
                node.position,
            )
        }

        val argResult = ctx.resolveType(node.argument, expectedType = "string")
        if (argResult is SemanticResult.Failure) {
            return argResult
        }

        val argType = (argResult as SemanticResult.Success).value
        if (argType != "string") {
            return SemanticResult.Failure(
                "Semantic Error: 'readEnv' argument must be a string, found '$argType'.",
                node.argument.position,
            )
        }

        val resolvedType = ctx.expectedType ?: "string"
        if (resolvedType !in ctx.rules.supportedTypes) {
            return SemanticResult.Failure(
                "Semantic Error: Type '$resolvedType' is not supported in PrintScript ${ctx.rules.version.label}.",
                node.position,
            )
        }

        return SemanticResult.Success(resolvedType)
    }
}
