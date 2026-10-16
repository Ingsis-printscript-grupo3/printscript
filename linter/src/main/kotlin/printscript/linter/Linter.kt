package printscript.linter

import printscript.ast.PositionedNode
import printscript.ast.Statement
import printscript.linter.rule.LinterRule

class Linter(private val rules: List<LinterRule>) : LinterInterface {
    override fun analyze(
        statements: Iterator<Statement>,
        onWarning: (Warning) -> Unit,
    ) {
        while (statements.hasNext()) {
            walk(statements.next()).forEach { node -> lint(node, onWarning) }
        }
    }

    // aca se decide que regla corre sobre que nodo
    private fun lint(
        node: PositionedNode,
        onWarning: (Warning) -> Unit,
    ) {
        for (rule in rules) {
            if (rule.appliesTo(node)) rule.check(node).forEach(onWarning)
        }
    }

    // pre-order: el nodo antes que sus hijos, asi el orden de los warnings sigue el del codigo.
    // Sequence y no List para no romper el streaming
    private fun walk(node: PositionedNode): Sequence<PositionedNode> =
        sequence {
            yield(node)
            childrenOf(node).forEach { yieldAll(walk(it)) }
        }
}
