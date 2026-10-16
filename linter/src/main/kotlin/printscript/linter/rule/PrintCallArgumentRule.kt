package printscript.linter.rule

import printscript.ast.PrintCall
import printscript.linter.Warning

class PrintCallArgumentRule : NodeRule<PrintCall>(PrintCall::class) {
    override fun checkNode(node: PrintCall): List<Warning> {
        if (node.value.isLiteralOrIdentifier()) return emptyList()
        return listOf(Warning(message = MESSAGE, position = node.position))
    }

    private companion object {
        const val MESSAGE = "println can only be called with an identifier or a literal, not an expression"
    }
}
