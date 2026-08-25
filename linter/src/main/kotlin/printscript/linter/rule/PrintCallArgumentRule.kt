package printscript.linter.rule

import printscript.ast.BinaryExpression
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.linter.Warning

class PrintCallArgumentRule : LinterRule {
    override fun check(statement: Statement): List<Warning> {
        val warnings = mutableListOf<Warning>()
        if (statement is PrintCall) {
            if (statement.value is BinaryExpression) {
                warnings.add(
                    Warning(
                        message = "println can only be called with an identifier or a literal, not an expression",
                        position = statement.position,
                    ),
                )
            }
        }
        return warnings
    }
}
