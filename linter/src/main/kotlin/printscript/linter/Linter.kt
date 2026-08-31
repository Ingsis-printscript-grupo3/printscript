package printscript.linter

import printscript.ast.Statement
import printscript.linter.rule.IdentifierFormatRule
import printscript.linter.rule.LinterRule
import printscript.linter.rule.PrintCallArgumentRule

class Linter(
    private val rules: List<LinterRule>,
) : LinterInterface {
    constructor(config: LinterRules = LinterRules()) : this(
        buildList {
            add(IdentifierFormatRule(config.identifierFormat))
            if (config.printCallArgumentsMustBeLiteralOrIdentifier) {
                add(PrintCallArgumentRule())
            }
        },
    )

    override fun analyze(statements: Iterator<Statement>): List<Warning> {
        val warnings = mutableListOf<Warning>()
        while (statements.hasNext()) {
            val statement = statements.next()
            rules.forEach { rule ->
                warnings.addAll(rule.check(statement))
            }
        }
        return warnings
    }
}
