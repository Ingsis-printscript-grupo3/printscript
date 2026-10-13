package printscript.linter.rule

import printscript.ast.PositionedNode
import printscript.linter.Warning

interface LinterRule {
    fun appliesTo(node: PositionedNode): Boolean

    fun check(node: PositionedNode): List<Warning>
}
