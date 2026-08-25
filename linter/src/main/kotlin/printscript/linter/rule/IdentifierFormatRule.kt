package printscript.linter.rule

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.linter.Warning

class IdentifierFormatRule(private val format: String) : LinterRule {
    override fun check(statement: Statement): List<Warning> {
        val warnings = mutableListOf<Warning>()
        if (statement is VariableDeclaration) {
            if (!isValidFormat(statement.name, format)) {
                warnings.add(
                    Warning(
                        message = "Identifier '${statement.name}' does not match format $format",
                        position = statement.position,
                    ),
                )
            }
        }
        return warnings
    }

    private fun isValidFormat(
        name: String,
        format: String,
    ): Boolean {
        return when (format) {
            "camel case" -> name.matches(Regex("^[a-z]+(?:[A-Z][a-z0-9]*)*$"))
            "snake case" -> name.matches(Regex("^[a-z]+(?:_[a-z0-9]+)*$"))
            else -> true // If unknown format, assume valid or throw? For now assume valid.
        }
    }
}
