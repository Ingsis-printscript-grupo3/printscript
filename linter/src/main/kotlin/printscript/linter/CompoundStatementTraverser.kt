package printscript.linter

import printscript.ast.Block
import printscript.ast.IfStatement
import printscript.ast.Statement

fun interface CompoundStatementTraverser {
    fun childrenOf(statement: Statement): Sequence<Statement>
}

class DefaultCompoundStatementTraverser(
    private val customHandlers: List<(Statement) -> Sequence<Statement>?> = emptyList(),
) : CompoundStatementTraverser {
    override fun childrenOf(statement: Statement): Sequence<Statement> {
        for (handler in customHandlers) {
            val children = handler(statement)
            if (children != null) return children
        }
        return when (statement) {
            is IfStatement ->
                sequence {
                    yield(statement.thenBranch)
                    statement.elseBranch?.let { yield(it) }
                }
            is Block -> statement.statements.asSequence()
            else -> emptySequence()
        }
    }
}
