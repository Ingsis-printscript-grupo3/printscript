package printscript.linter.rule

import printscript.ast.PositionedNode
import printscript.ast.ReadInput
import printscript.linter.Warning

class ReadInputArgumentRule : LinterRule {
    override fun appliesTo(node: PositionedNode): Boolean = node is ReadInput

    override fun check(node: PositionedNode): List<Warning> {
        if (node !is ReadInput) return emptyList()
        if (node.argument.isLiteralOrIdentifier()) return emptyList()
        return listOf(Warning(message = MESSAGE, position = node.position))
    }

    private companion object {
        const val MESSAGE = "readInput can only be called with an identifier or a literal, not an expression"
    }
}
