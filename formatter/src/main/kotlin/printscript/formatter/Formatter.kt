package printscript.formatter

import printscript.common.Token
import printscript.common.TokenType
import java.io.Writer

private val OPERATORS = setOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE)
private val STATEMENT_BOUNDARIES = setOf(TokenType.SEMICOLON, TokenType.LEFTBRACE, TokenType.RIGHTBRACE)

class Formatter(
    private val rules: FormatterRules,
) : FormatterInterface {
    override fun format(
        tokens: Iterator<Token>,
        output: Writer,
    ) {
        val state = FormatState()
        for (token in tokens) {
            if (token.type == TokenType.EOF) break
            if (token.type == TokenType.RIGHTBRACE) state.depth--
            val previous = state.previous
            if (previous != null) output.write(spaceBetween(previous, token, state))
            output.write(textOf(token))
            state.moveTo(token)
        }
        output.flush()
    }

    // arranca con el espacio que habia en el archivo y cada regla activa lo pisa
    private fun spaceBetween(
        previous: Token,
        current: Token,
        state: FormatState,
    ): String {
        val gap = originalGap(previous, current)
        applyLineBreakRules(previous, state, gap)
        applyBraceRules(current, state, gap)
        if (gap.newlines == 0) {
            applyColonRules(previous, current, gap)
            applyEqualsAndOperatorRules(previous, current, gap)
        } else {
            rules.indentInsideIf?.let { gap.spaces = state.depth * it }
            state.lineIndent = gap.spaces
        }
        return "\n".repeat(gap.newlines) + " ".repeat(gap.spaces)
    }

    private fun originalGap(
        previous: Token,
        current: Token,
    ): Gap =
        if (current.start.line == previous.end.line) {
            Gap(newlines = 0, spaces = current.start.column - previous.end.column)
        } else {
            Gap(newlines = current.start.line - previous.end.line, spaces = current.start.column - 1)
        }

    private fun applyLineBreakRules(
        previous: Token,
        state: FormatState,
        gap: Gap,
    ) {
        if (previous.type != TokenType.SEMICOLON) return
        val breaksAfterPrintln = rules.lineBreaksAfterPrintln
        if (breaksAfterPrintln != null && state.statementIsPrintln) {
            gap.breakLine(1 + breaksAfterPrintln, state.lineIndent)
        } else if (rules.lineBreakAfterStatement && gap.newlines == 0) {
            gap.breakLine(1, state.lineIndent)
        }
    }

    private fun applyBraceRules(
        current: Token,
        state: FormatState,
        gap: Gap,
    ) {
        if (current.type != TokenType.LEFTBRACE) return
        if (rules.ifBraceSameLine) {
            gap.newlines = 0
            gap.spaces = 1
        } else if (rules.ifBraceBelowLine && gap.newlines == 0) {
            gap.breakLine(1, state.lineIndent)
        }
    }

    private fun applyColonRules(
        previous: Token,
        current: Token,
        gap: Gap,
    ) {
        if (rules.singleSpaceSeparation) gap.spaces = if (current.type == TokenType.SEMICOLON) 0 else 1
        if (rules.spaceBeforeColon && current.type == TokenType.COLON) gap.spaces = 1
        if (rules.spaceAfterColon && previous.type == TokenType.COLON) gap.spaces = 1
    }

    private fun applyEqualsAndOperatorRules(
        previous: Token,
        current: Token,
        gap: Gap,
    ) {
        val nextToEquals = previous.type == TokenType.ASSIGN || current.type == TokenType.ASSIGN
        if (nextToEquals && rules.spacingAroundEquals) gap.spaces = 1
        if (nextToEquals && rules.noSpacingAroundEquals) gap.spaces = 0
        val nextToOperator = previous.type in OPERATORS || current.type in OPERATORS
        if (nextToOperator && rules.spaceSurroundingOperations) gap.spaces = 1
    }

    // el lexer le saca las comillas a los strings, hay que volver a ponerlas
    private fun textOf(token: Token): String =
        if (token.type == TokenType.STRINGLITERAL) "\"${token.value}\"" else token.value
}

private class Gap(var newlines: Int, var spaces: Int) {
    fun breakLine(
        lines: Int,
        indent: Int,
    ) {
        if (newlines == 0) spaces = indent
        newlines = lines
    }
}

private class FormatState {
    var previous: Token? = null
    var depth = 0
    var lineIndent = 0
    var statementIsPrintln = false

    fun moveTo(token: Token) {
        val previousType = previous?.type
        if (previousType == null || previousType in STATEMENT_BOUNDARIES) {
            statementIsPrintln = token.type == TokenType.PRINTLN
        }
        if (token.type == TokenType.LEFTBRACE) depth++
        previous = token
    }
}
