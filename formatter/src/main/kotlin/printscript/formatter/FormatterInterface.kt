package printscript.formatter

import printscript.ast.Statement
import java.io.Writer

interface FormatterInterface {
    fun format(
        statements: Iterator<Statement>,
        output: Writer,
    )
}
