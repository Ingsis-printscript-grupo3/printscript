package printscript.semantic.handler.expression

import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult

class BooleanLiteralHandler : Handler<Expression, ExpressionResolver, SemanticResult<String>> {
    override fun applies(node: Expression) = node is BooleanLiteral

    override fun handle(
        node: Expression,
        ctx: ExpressionResolver,
    ): SemanticResult<String> {
        if (node !is BooleanLiteral) {
            return SemanticResult.Failure("Semantic Error: Unexpected node in BooleanLiteralHandler.", node.position)
        }
        if (!ctx.rules.allowsBooleans) {
            return SemanticResult.Failure(
                "Semantic Error: Booleans are not supported in PrintScript ${ctx.rules.version.label}.",
                node.position,
            )
        }
        return SemanticResult.Success("boolean")
    }
}
