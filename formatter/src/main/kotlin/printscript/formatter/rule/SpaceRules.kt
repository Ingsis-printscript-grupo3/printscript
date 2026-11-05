package printscript.formatter.rule

import printscript.common.TokenType
import printscript.formatter.Gap

private val OPERATORS = setOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE)

// las de espacio solo miran tokens que quedaron en la misma linea: entre lineas manda la indentacion
private fun Gap.touches(types: Set<TokenType>): Boolean = previous.type in types || current.type in types

internal object SingleSpaceSeparation : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.isSameLine

    override fun apply(gap: Gap) {
        gap.spaces = if (gap.current.type == TokenType.SEMICOLON) 0 else 1
    }
}

internal object SpaceBeforeColon : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.isSameLine && gap.current.type == TokenType.COLON

    override fun apply(gap: Gap) {
        gap.spaces = 1
    }
}

internal object SpaceAfterColon : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.isSameLine && gap.previous.type == TokenType.COLON

    override fun apply(gap: Gap) {
        gap.spaces = 1
    }
}

internal object SpacingAroundEquals : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.isSameLine && gap.touches(setOf(TokenType.ASSIGN))

    override fun apply(gap: Gap) {
        gap.spaces = 1
    }
}

internal object NoSpacingAroundEquals : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.isSameLine && gap.touches(setOf(TokenType.ASSIGN))

    override fun apply(gap: Gap) {
        gap.spaces = 0
    }
}

internal object SpaceSurroundingOperations : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.isSameLine && gap.touches(OPERATORS)

    override fun apply(gap: Gap) {
        gap.spaces = 1
    }
}
