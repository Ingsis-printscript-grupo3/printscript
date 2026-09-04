package printscript.linter.rule

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.linter.CAMEL_CASE
import printscript.linter.VALID_IDENTIFIER_FORMATS
import printscript.linter.Warning

class IdentifierFormatRule(private val format: String) : LinterRule {
    init {
        require(format in VALID_IDENTIFIER_FORMATS) {
            "identifierFormat must be one of $VALID_IDENTIFIER_FORMATS, was '$format'"
        }
    }

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
            CAMEL_CASE -> name.matches(Regex("^[a-z]+(?:[A-Z][a-z0-9]*)*$"))
            else -> name.matches(Regex("^[a-z]+(?:_[a-z0-9]+)*$"))
        }
    }
}
