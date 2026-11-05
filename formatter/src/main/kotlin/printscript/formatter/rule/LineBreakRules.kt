package printscript.formatter.rule

import printscript.common.TokenType
import printscript.formatter.Gap

internal class LineBreaksAfterPrintln(private val blankLines: Int) : FormatterRule {
    override fun appliesTo(gap: Gap) =
        gap.previous.type == TokenType.SEMICOLON && gap.state.statementStart?.type == TokenType.PRINTLN

    override fun apply(gap: Gap) {
        gap.breakLine(1 + blankLines)
    }
}

internal object LineBreakAfterStatement : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.previous.type == TokenType.SEMICOLON && gap.isSameLine

    override fun apply(gap: Gap) {
        gap.breakLine(1)
    }
}
