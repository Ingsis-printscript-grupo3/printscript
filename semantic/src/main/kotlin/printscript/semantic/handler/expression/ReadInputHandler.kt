package printscript.semantic.handler.expression

import printscript.ast.Expression
import printscript.ast.ReadInput
import printscript.ast.registry.Handler
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult

class ReadInputHandler : Handler<Expression, ExpressionResolver, SemanticResult<String>> {
    override fun applies(node: Expression) = node is ReadInput

    override fun handle(
        node: Expression,
        ctx: ExpressionResolver,
    ): SemanticResult<String> {
        if (node !is ReadInput) {
            return SemanticResult.Failure(
                "Semantic Error: Unexpected node in ReadInputHandler.",
                node.position,
            )
        }

        if (!ctx.rules.allowsReadInput) {
            return SemanticResult.Failure(
                "Semantic Error: 'readInput' is not supported in PrintScript ${ctx.rules.version.label}.",
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
                "Semantic Error: 'readInput' argument must be a string, found '$argType'.",
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
