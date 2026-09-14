package printscript.linter.rule

import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.linter.Warning

class PrintCallArgumentRule : LinterRule {
    override fun check(statement: Statement): List<Warning> {
        if (statement !is PrintCall) return emptyList()
        if (statement.value.isLiteralOrIdentifier()) return emptyList()
        return listOf(Warning(message = MESSAGE, position = statement.position))
    }

    private companion object {
        const val MESSAGE = "println can only be called with an identifier or a literal, not an expression"
    }
}
