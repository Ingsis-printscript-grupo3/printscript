package printscript.linter.rule

import printscript.ast.PositionedNode
import printscript.ast.PrintCall
import printscript.linter.Warning

class PrintCallArgumentRule : LinterRule {
    override fun appliesTo(node: PositionedNode): Boolean = node is PrintCall

    override fun check(node: PositionedNode): List<Warning> {
        val printCall = node as PrintCall
        if (printCall.value.isLiteralOrIdentifier()) return emptyList()
        return listOf(Warning(message = MESSAGE, position = printCall.position))
    }

    private companion object {
        const val MESSAGE = "println can only be called with an identifier or a literal, not an expression"
    }
}
