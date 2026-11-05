package printscript.semantic.handler.expression

import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.ast.registry.Handler
import printscript.semantic.ResolverContext
import printscript.semantic.SemanticResult

class NumberLiteralHandler : Handler<Expression, ResolverContext, SemanticResult<String>> {
    override fun applies(node: Expression) = node is NumberLiteral

    override fun handle(
        node: Expression,
        ctx: ResolverContext,
    ): SemanticResult<String> {
        if (node !is NumberLiteral) {
            return SemanticResult.Failure("Unexpected node in NumberLiteralHandler.", node.position)
        }
        return SemanticResult.Success("number")
    }
}
