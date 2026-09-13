package printscript.formatter.rule

import printscript.common.TokenType
import printscript.formatter.Gap

internal object IfBraceSameLine : FormatterRule {
    override fun apply(gap: Gap) {
        gap.newlines = 0
        gap.spaces = 1
    }
}

internal object IfBraceBelowLine : FormatterRule {
    override fun apply(gap: Gap) {
        if (gap.newlines == 0) gap.breakLine(1)
    }
}

internal class IndentInsideIf(private val size: Int) : FormatterRule {
    override fun apply(gap: Gap) {
        // la llave que cierra queda donde estaba, como en el TCK
        if (gap.current.type != TokenType.RIGHTBRACE) gap.spaces = gap.state.depth * size
    }
}
