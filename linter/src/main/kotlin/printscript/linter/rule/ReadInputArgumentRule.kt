package printscript.linter.rule

import printscript.ast.PositionedNode
import printscript.ast.ReadInput
import printscript.linter.Warning

class ReadInputArgumentRule : LinterRule {
    override fun appliesTo(node: PositionedNode): Boolean = node is ReadInput

    override fun check(node: PositionedNode): List<Warning> {
        val readInput = node as ReadInput
        if (readInput.argument.isLiteralOrIdentifier()) return emptyList()
        return listOf(Warning(message = MESSAGE, position = readInput.position))
    }

    private companion object {
        const val MESSAGE = "readInput can only be called with an identifier or a literal, not an expression"
    }
}
