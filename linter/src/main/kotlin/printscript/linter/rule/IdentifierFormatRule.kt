package printscript.linter.rule

import printscript.ast.Assignment
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
        val name = declaredOrAssignedName(statement) ?: return emptyList()
        if (isValidFormat(name, format)) return emptyList()
        return listOf(
            Warning(
                message = "Identifier '$name' does not match format $format",
                position = statement.position,
            ),
        )
    }

    // VariableDeclaration cubre let y const, el parser no hace un nodo aparte para const
    private fun declaredOrAssignedName(statement: Statement): String? =
        when (statement) {
            is VariableDeclaration -> statement.name
            is Assignment -> statement.name
            else -> null
        }

    private fun isValidFormat(
        name: String,
        format: String,
    ): Boolean {
        return when (format) {
            CAMEL_CASE -> name.matches(Regex("^[a-z][a-z0-9]*(?:[A-Z][a-z0-9]*)*$"))
            else -> name.matches(Regex("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$"))
        }
    }
}
