package printscript.linter

import printscript.ast.Statement
import printscript.linter.rule.LinterRule
import printscript.linter.rule.LinterRuleRegistry

// una sola regla: si son varias vienen agrupadas en un CompositeRule
class Linter(
    private val rule: LinterRule,
    private val traverser: CompoundStatementTraverser = DefaultCompoundStatementTraverser(),
) : LinterInterface {
    constructor(config: LinterRules = LinterRules()) : this(LinterRuleRegistry().ruleFor(config))

    override fun analyze(
        statements: Iterator<Statement>,
        onWarning: (Warning) -> Unit,
    ) {
        while (statements.hasNext()) {
            flatten(statements.next()).forEach { statement ->
                rule.check(statement).forEach(onWarning)
            }
        }
    }

    // el recorrido vive aca y no en cada regla, asi las reglas ven un statement a la vez
    // y no repiten la logica de desempaquetar bloques.
    // Sequence y no List: el pipeline es streaming, aplanar entero romperia esa garantia
    private fun flatten(statement: Statement): Sequence<Statement> =
        sequence {
            yield(statement)
            traverser.childrenOf(statement).forEach { child ->
                yieldAll(flatten(child))
            }
        }
}
