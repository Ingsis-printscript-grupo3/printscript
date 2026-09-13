package printscript.formatter.rule

import printscript.common.TokenType
import printscript.formatter.Gap

private val OPERATORS = setOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE)

internal object SingleSpaceSeparation : FormatterRule {
    override fun apply(gap: Gap) {
        gap.spaces = if (gap.current.type == TokenType.SEMICOLON) 0 else 1
    }
}

internal object SpaceBeforeColon : FormatterRule {
    override fun apply(gap: Gap) {
        if (gap.current.type == TokenType.COLON) gap.spaces = 1
    }
}

internal object SpaceAfterColon : FormatterRule {
    override fun apply(gap: Gap) {
        if (gap.previous.type == TokenType.COLON) gap.spaces = 1
    }
}

internal object SpacingAroundEquals : FormatterRule {
    override fun apply(gap: Gap) {
        if (gap.previous.type == TokenType.ASSIGN || gap.current.type == TokenType.ASSIGN) gap.spaces = 1
    }
}

internal object NoSpacingAroundEquals : FormatterRule {
    override fun apply(gap: Gap) {
        if (gap.previous.type == TokenType.ASSIGN || gap.current.type == TokenType.ASSIGN) gap.spaces = 0
    }
}

internal object SpaceSurroundingOperations : FormatterRule {
    override fun apply(gap: Gap) {
        if (gap.previous.type in OPERATORS || gap.current.type in OPERATORS) gap.spaces = 1
    }
}
