package printscript.formatter.rule

import printscript.common.TokenType
import printscript.formatter.FormatterRules

// arma el arbol con las reglas prendidas en la config, en el orden en que se pisan
internal object FormatterRuleTree {
    fun from(config: FormatterRules): FormatterRule =
        RuleGroup(
            listOf(
                RuleGroup(afterSemicolon(config)) { it.previous.type == TokenType.SEMICOLON },
                RuleGroup(beforeLeftBrace(config)) { it.current.type == TokenType.LEFTBRACE },
                RuleGroup(sameLine(config)) { it.newlines == 0 },
                RuleGroup(newLine(config)) { it.newlines > 0 },
            ),
        )

    private fun afterSemicolon(config: FormatterRules): List<FormatterRule> =
        listOfNotNull(
            config.lineBreaksAfterPrintln?.let { LineBreaksAfterPrintln(it) },
            LineBreakAfterStatement.takeIf { config.lineBreakAfterStatement },
        )

    private fun beforeLeftBrace(config: FormatterRules): List<FormatterRule> =
        listOfNotNull(
            IfBraceSameLine.takeIf { config.ifBraceSameLine },
            IfBraceBelowLine.takeIf { config.ifBraceBelowLine },
        )

    private fun sameLine(config: FormatterRules): List<FormatterRule> =
        listOfNotNull(
            SingleSpaceSeparation.takeIf { config.singleSpaceSeparation },
            SpaceBeforeColon.takeIf { config.spaceBeforeColon },
            SpaceAfterColon.takeIf { config.spaceAfterColon },
            SpacingAroundEquals.takeIf { config.spacingAroundEquals },
            NoSpacingAroundEquals.takeIf { config.noSpacingAroundEquals },
            SpaceSurroundingOperations.takeIf { config.spaceSurroundingOperations },
        )

    private fun newLine(config: FormatterRules): List<FormatterRule> =
        listOfNotNull(config.indentInsideIf?.let { IndentInsideIf(it) })
}
