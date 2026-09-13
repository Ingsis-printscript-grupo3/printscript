package printscript.formatter

import printscript.common.Token
import printscript.common.TokenType
import printscript.formatter.rule.FormatterRuleTree
import java.io.Writer

private val STATEMENT_BOUNDARIES = setOf(TokenType.SEMICOLON, TokenType.LEFTBRACE, TokenType.RIGHTBRACE)

class Formatter(
    rules: FormatterRules,
) : FormatterInterface {
    private val rule = FormatterRuleTree.from(rules)

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
        val gap = Gap(previous, current, state)
        rule.apply(gap)
        if (gap.newlines > 0) state.lineIndent = gap.spaces
        return "\n".repeat(gap.newlines) + " ".repeat(gap.spaces)
    }

    // el lexer le saca las comillas a los strings, hay que volver a ponerlas
    private fun textOf(token: Token): String =
        if (token.type == TokenType.STRINGLITERAL) "\"${token.value}\"" else token.value
}

internal class Gap(
    val previous: Token,
    val current: Token,
    val state: FormatState,
) {
    var newlines = current.start.line - previous.end.line
    var spaces = if (newlines == 0) current.start.column - previous.end.column else current.start.column - 1

    fun breakLine(lines: Int) {
        if (newlines == 0) spaces = state.lineIndent
        newlines = lines
    }
}

internal class FormatState {
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
