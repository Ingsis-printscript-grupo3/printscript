package printscript.semantic.handler.expression

import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.ast.registry.Handler
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult

class NumberLiteralHandler : Handler<Expression, ExpressionResolver, SemanticResult<String>> {
    override fun applies(node: Expression) = node is NumberLiteral

    override fun handle(
        node: Expression,
        ctx: ExpressionResolver,
    ): SemanticResult<String> {
        if (node !is NumberLiteral) {
            return SemanticResult.Failure("Semantic Error: Unexpected node in NumberLiteralHandler.", node.position)
        }
        return SemanticResult.Success("number")
    }
}
