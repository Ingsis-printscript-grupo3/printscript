package printscript.linter.rule

import printscript.ast.Statement
import printscript.linter.Warning

// un grupo de reglas que se usa como si fuera una sola, asi el Linter no sabe cuantas hay.
// como es una LinterRule mas, un grupo puede contener otro grupo
class CompositeRule(
    private val rules: List<LinterRule>,
) : LinterRule {
    override fun check(statement: Statement): List<Warning> = rules.flatMap { it.check(statement) }
}
