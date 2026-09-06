package printscript.semantic.handler.expression

import printscript.ast.Expression
import printscript.ast.StringLiteral
import printscript.ast.registry.Handler
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult

class StringLiteralHandler : Handler<Expression, ExpressionResolver, SemanticResult<String>> {
    override fun applies(node: Expression) = node is StringLiteral

    override fun handle(
        node: Expression,
        ctx: ExpressionResolver,
    ): SemanticResult<String> {
        if (node !is StringLiteral) {
            return SemanticResult.Failure("Semantic Error: Unexpected node in StringLiteralHandler.", node.position)
        }
        return SemanticResult.Success("string")
    }
}
