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
            return SemanticResult.Failure("Unexpected node in IdentifierHandler.", node.position)
        }
        return when (val result = ctx.symbolTable.lookupType(node.name)) {
            is SemanticResult.Failure -> SemanticResult.Failure(result.message, node.position)
            is SemanticResult.Success -> result
        }
    }
}
