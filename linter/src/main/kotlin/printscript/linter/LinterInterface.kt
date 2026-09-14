package printscript.linter

import printscript.ast.Statement

interface LinterInterface {
    fun analyze(
        statements: Iterator<Statement>,
        onWarning: (Warning) -> Unit,
    )
}
