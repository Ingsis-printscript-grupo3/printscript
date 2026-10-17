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
        return resolveReadArgument("readEnv", node.argument, ctx)
    }
}
