package printscript.semantic.handler.expression

import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.registry.Handler
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult

class IdentifierHandler : Handler<Expression, ExpressionResolver, SemanticResult<String>> {
    override fun applies(node: Expression) = node is Identifier

    override fun handle(
        node: Expression,
        ctx: ExpressionResolver,
    ): SemanticResult<String> {
        if (node !is Identifier) {
            return SemanticResult.Failure("Semantic Error: Unexpected node in IdentifierHandler.", node.position)
        }
        val result = ctx.symbolTable.lookupType(node.name)
        return if (result is SemanticResult.Failure) {
            SemanticResult.Failure(result.message, node.position)
        } else {
            result
        }
    }
}
