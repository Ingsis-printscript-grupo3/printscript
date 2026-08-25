import printscript.ast.Statement

interface Formatter {
    fun format(statements: List<Statement>): String
}
