package printscript.semantic.handler.statement

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Handler
import printscript.semantic.SemanticResult
import printscript.semantic.StatementValidator

class VariableDeclarationHandler : Handler<Statement, StatementValidator, SemanticResult<Unit>> {
    override fun applies(node: Statement) = node is VariableDeclaration

    override fun handle(
        node: Statement,
        ctx: StatementValidator,
    ): SemanticResult<Unit> {
        if (node !is VariableDeclaration) {
            return SemanticResult.Failure(
                "Semantic Error: Unexpected node in VariableDeclarationHandler.",
                node.position,
            )
        }

        val typeMismatch =
            node.value?.let { value ->
                val exprResult = ctx.expressionResolver.resolveType(value)
                val exprType = (exprResult as? SemanticResult.Success)?.value
                when {
                    exprResult is SemanticResult.Failure -> exprResult
                    exprType != node.type -> SemanticResult.Failure("Incompatible types.")
                    else -> null
                }
            }

        return typeMismatch ?: ctx.symbolTable.define(node.name, node.type)
    }
}
