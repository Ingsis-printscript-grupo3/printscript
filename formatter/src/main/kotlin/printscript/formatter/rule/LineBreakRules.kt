package printscript.formatter.rule

import printscript.formatter.Gap

internal class LineBreaksAfterPrintln(private val blankLines: Int) : FormatterRule {
    override fun apply(gap: Gap) {
        if (gap.state.statementIsPrintln) gap.breakLine(1 + blankLines)
    }
}

internal object LineBreakAfterStatement : FormatterRule {
    override fun apply(gap: Gap) {
        if (gap.newlines == 0) gap.breakLine(1)
    }
}
