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
            return SemanticResult.Failure("Unexpected node in ReadInputHandler.", node.position)
        }
        return resolveReadArgument("readInput", node.argument, ctx)
    }
}
