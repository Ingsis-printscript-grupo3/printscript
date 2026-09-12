package printscript.linter

import printscript.ast.Statement
import printscript.linter.rule.IdentifierFormatRule
import printscript.linter.rule.LinterRule
import printscript.linter.rule.PrintCallArgumentRule
import printscript.linter.rule.ReadInputCallArgumentRule

class Linter(
    private val rules: List<LinterRule>,
) : LinterInterface {
    constructor(config: LinterRules = LinterRules()) : this(
        buildList {
            if (config.hasIdentifierFormat) {
                add(IdentifierFormatRule(config.identifierFormat))
            }
            if (config.printCallArgumentsMustBeLiteralOrIdentifier && config.hasPrintCallArguments) {
                add(PrintCallArgumentRule())
            }
            if (config.readInputArgumentsMustBeLiteralOrIdentifier && config.hasReadInputArguments) {
                add(ReadInputCallArgumentRule())
            }
        },
    )

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
