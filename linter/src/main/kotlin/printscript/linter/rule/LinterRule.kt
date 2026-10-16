package printscript.linter.rule

import printscript.ast.Statement
import printscript.linter.Warning

interface LinterRule {
    fun check(statement: Statement): List<Warning>
}
