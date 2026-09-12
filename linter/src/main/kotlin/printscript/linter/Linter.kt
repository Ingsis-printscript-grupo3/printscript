package printscript.linter

import printscript.ast.Statement
import printscript.linter.rule.LinterRule
import printscript.linter.rule.LinterRuleRegistry

class Linter(
    private val rules: List<LinterRule>,
) : LinterInterface {
    constructor(config: LinterRules = LinterRules()) : this(LinterRuleRegistry.rulesFor(config))

    override fun analyze(
        statements: Iterator<Statement>,
        onWarning: (Warning) -> Unit,
    ) {
        while (statements.hasNext()) {
            val statement = statements.next()
            rules.forEach { rule ->
                rule.check(statement).forEach(onWarning)
            }
        }
    }
}
