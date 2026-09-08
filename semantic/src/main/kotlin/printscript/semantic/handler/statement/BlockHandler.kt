package printscript.semantic.handler.statement

import printscript.ast.Block
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.semantic.SemanticResult
import printscript.semantic.StatementValidator

class BlockHandler : Handler<Statement, StatementValidator, SemanticResult<Unit>> {
    override fun applies(node: Statement) = node is Block

    override fun handle(
        node: Statement,
        ctx: StatementValidator,
    ): SemanticResult<Unit> {
        if (node !is Block) {
            return SemanticResult.Failure("Semantic Error: Unexpected node in BlockHandler.", node.position)
        }

        ctx.symbolTable.enterScope()
        try {
            for (statement in node.statements) {
                val result = ctx.validate(statement)
                if (result is SemanticResult.Failure) {
                    return result
                }
            }
            return SemanticResult.Success(Unit)
        } finally {
            ctx.symbolTable.exitScope()
        }
    }
}
