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
            return SemanticResult.Failure("Semantic Error: Unexpected node in IfStatementHandler.", node.position)
        }

        if (!ctx.rules.allowsConditionals) {
            return SemanticResult.Failure(
                "Semantic Error: 'if' statements are not supported in PrintScript ${ctx.rules.version.label}.",
                node.position,
            )
        }

        val conditionResult = ctx.expressionResolver.resolveType(node.condition)
        if (conditionResult is SemanticResult.Failure) {
            return conditionResult
        }

        val conditionType = (conditionResult as SemanticResult.Success).value
        if (conditionType != "boolean") {
            return SemanticResult.Failure(
                "Semantic Error: 'if' condition must be a boolean expression, found '$conditionType'.",
                node.condition.position,
            )
        }

        val thenResult = ctx.validate(node.thenBranch)
        if (thenResult is SemanticResult.Failure) {
            return thenResult
        }

        val elseBranch = node.elseBranch
        if (elseBranch != null) {
            val elseResult = ctx.validate(elseBranch)
            if (elseResult is SemanticResult.Failure) {
                return elseResult
            }
        }

        return SemanticResult.Success(Unit)
    }
}
