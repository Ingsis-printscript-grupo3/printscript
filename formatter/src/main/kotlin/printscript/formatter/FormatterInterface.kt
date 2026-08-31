package printscript.formatter
import printscript.ast.Statement

interface FormatterInterface {
    fun format(statements: List<Statement>): String
}
