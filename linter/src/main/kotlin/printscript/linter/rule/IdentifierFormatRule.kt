package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.linter.CAMEL_CASE
import printscript.linter.VALID_IDENTIFIER_FORMATS
import printscript.linter.Warning

class IdentifierFormatRule(private val format: String) : LinterRule {
    init {
        require(format in VALID_IDENTIFIER_FORMATS) {
            "identifierFormat must be one of $VALID_IDENTIFIER_FORMATS, was '$format'"
        }
    }

    override fun check(statement: Statement): List<Warning> =
        when (statement) {
            // VariableDeclaration cubre let y const, el parser no hace un nodo aparte para const.
            // el warning va al nombre, no al let: en Assignment la posicion ya es la del nombre
            is VariableDeclaration -> checkIdentifier(statement.name, statement.namePosition)
            is Assignment -> checkIdentifier(statement.name, statement.position)
            else -> emptyList()
        }

    private fun checkIdentifier(
        name: String,
        position: Position,
    ): List<Warning> {
        if (isValidFormat(name)) return emptyList()
        return listOf(
            Warning(
                message = "Identifier '$name' does not match format $format",
                position = position,
            ),
        )
    }

    private fun isValidFormat(name: String): Boolean =
        when (format) {
            CAMEL_CASE -> name.matches(CAMEL_CASE_PATTERN)
            else -> name.matches(SNAKE_CASE_PATTERN)
        }

    // compiladas una sola vez y no en cada identificador que se chequea
    private companion object {
        private val CAMEL_CASE_PATTERN = Regex("^[a-z][a-z0-9]*(?:[A-Z][a-z0-9]*)*$")
        private val SNAKE_CASE_PATTERN = Regex("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$")
    }
}
