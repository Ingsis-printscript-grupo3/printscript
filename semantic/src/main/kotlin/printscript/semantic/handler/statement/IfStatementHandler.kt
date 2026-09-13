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

        when (val conditionResult = ctx.expressionResolver.resolveType(node.condition)) {
            is SemanticResult.Failure -> return conditionResult
            is SemanticResult.Success -> {
                if (conditionResult.value != "boolean") {
                    return SemanticResult.Failure(
                        "'if' condition must be a boolean expression, found '${conditionResult.value}'.",
                        node.condition.position,
                    )
                }
            }
        }

        when (val thenResult = ctx.validate(node.thenBranch)) {
            is SemanticResult.Failure -> return thenResult
            is SemanticResult.Success -> Unit
        }

        val elseBranch = node.elseBranch
        if (elseBranch != null) {
            when (val elseResult = ctx.validate(elseBranch)) {
                is SemanticResult.Failure -> return elseResult
                is SemanticResult.Success -> Unit
            }
        }

        return SemanticResult.Success(Unit)
    }
}
