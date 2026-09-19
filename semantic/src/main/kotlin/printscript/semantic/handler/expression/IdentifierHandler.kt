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
        return ctx.symbolTable.lookupType(node.name, at = node.position)
    }
}
