package printscript.semantic.handler.statement

import printscript.ast.IfStatement
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.semantic.SemanticResult
import printscript.semantic.StatementValidator

class IfStatementHandler : Handler<Statement, StatementValidator, SemanticResult<Unit>> {
    override fun applies(node: Statement) = node is IfStatement

    override fun handle(
        node: Statement,
        ctx: StatementValidator,
    ): SemanticResult<Unit> {
        if (node !is IfStatement) {
            return SemanticResult.Failure("Unexpected node in IfStatementHandler.", node.position)
        }
        val conditionResult = validateCondition(node, ctx)
        if (conditionResult is SemanticResult.Failure) return conditionResult

        val thenResult = ctx.validate(node.thenBranch)
        if (thenResult is SemanticResult.Failure) return thenResult

        return validateElseBranch(node.elseBranch, ctx)
    }

    private fun validateCondition(
        node: IfStatement,
        ctx: StatementValidator,
    ): SemanticResult<Unit> =
        when (val conditionResult = ctx.expressionResolver.resolveType(node.condition)) {
            is SemanticResult.Failure -> conditionResult
            is SemanticResult.Success ->
                if (conditionResult.value != "boolean") {
                    SemanticResult.Failure(
                        "'if' condition must be a boolean expression, found '${conditionResult.value}'.",
                        node.condition.position,
                    )
                } else {
                    SemanticResult.Success(Unit)
                }
        }

    private fun validateElseBranch(
        elseBranch: Statement?,
        ctx: StatementValidator,
    ): SemanticResult<Unit> {
        if (elseBranch == null) return SemanticResult.Success(Unit)
        return ctx.validate(elseBranch)
    }
}
