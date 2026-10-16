package printscript.linter.rule

import printscript.ast.ReadInput
import printscript.linter.Warning

class ReadInputArgumentRule : NodeRule<ReadInput>(ReadInput::class) {
    override fun checkNode(node: ReadInput): List<Warning> {
        if (node.argument.isLiteralOrIdentifier()) return emptyList()
        return listOf(Warning(message = MESSAGE, position = node.position))
    }

    private companion object {
        const val MESSAGE = "readInput can only be called with an identifier or a literal, not an expression"
    }
}
