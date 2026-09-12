package printscript.linter

import printscript.ast.Block
import printscript.ast.IfStatement
import printscript.ast.Statement
import printscript.linter.rule.LinterRule
import printscript.linter.rule.LinterRuleRegistry

class Linter(
    private val rules: List<LinterRule>,
) : LinterInterface {
    constructor(config: LinterRules = LinterRules()) : this(LinterRuleRegistry().rulesFor(config))

    override fun analyze(
        statements: Iterator<Statement>,
        onWarning: (Warning) -> Unit,
    ) {
        while (statements.hasNext()) {
            flatten(statements.next()).forEach { statement ->
                rules.forEach { rule ->
                    rule.check(statement).forEach(onWarning)
                }
            }
        }
    }

    // el recorrido vive aca y no en cada regla, asi las reglas ven un statement a la vez
    // y no repiten la logica de desempaquetar bloques.
    // Sequence y no List: el pipeline es streaming, aplanar entero romperia esa garantia
    private fun flatten(statement: Statement): Sequence<Statement> =
        sequence {
            yield(statement)
            when (statement) {
                is IfStatement -> {
                    yieldAll(flatten(statement.thenBranch))
                    statement.elseBranch?.let { yieldAll(flatten(it)) }
                }
                is Block -> statement.statements.forEach { yieldAll(flatten(it)) }
                else -> Unit
            }
        }
}
