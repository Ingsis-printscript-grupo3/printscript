package printscript.formatter.rule

import printscript.common.TokenType
import printscript.formatter.Gap

internal object IfBraceSameLine : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.current.type == TokenType.LEFTBRACE

    override fun apply(gap: Gap) {
        gap.newlines = 0
        gap.spaces = 1
    }
}

internal object IfBraceBelowLine : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.current.type == TokenType.LEFTBRACE && gap.isSameLine

    override fun apply(gap: Gap) {
        gap.breakLine(1)
    }
}

// corre despues de los saltos de linea: indenta tambien las lineas que crearon otras reglas
internal class IndentInsideIf(private val size: Int) : FormatterRule {
    override fun appliesTo(gap: Gap) = !gap.isSameLine

    override fun apply(gap: Gap) {
        gap.spaces = gap.state.depth * size
    }
}
