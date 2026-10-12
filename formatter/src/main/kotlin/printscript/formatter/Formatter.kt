package printscript.formatter

import printscript.common.Token
import printscript.common.TokenType
import printscript.formatter.rule.FormatterRule
import java.io.Writer

private val STATEMENT_BOUNDARIES = setOf(TokenType.SEMICOLON, TokenType.LEFTBRACE, TokenType.RIGHTBRACE)

// no conoce ninguna regla: recibe la lista ya armada y en orden. Quien elige cuales van es FormatterFactory
class Formatter(
    private val rules: List<FormatterRule>,
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

    // arranca con el espacio que habia en el archivo y cada regla que aplica lo pisa
    private fun spaceBetween(
        previous: Token,
        current: Token,
        state: FormatState,
    ): String {
        val gap = Gap(previous, current, state)
        for (rule in rules) {
            if (rule.appliesTo(gap)) rule.apply(gap)
        }
        if (gap.newlines > 0) state.lineIndent = gap.spaces
        return "\n".repeat(gap.newlines) + " ".repeat(gap.spaces)
    }

    // el lexer le saca las comillas a los strings, hay que volver a ponerlas
    private fun textOf(token: Token): String =
        if (token.type == TokenType.STRINGLITERAL) "\"${token.value}\"" else token.value
}

// el espacio entre dos tokens: las reglas lo leen para decidir si aplican y lo modifican
class Gap internal constructor(
    val previous: Token,
    val current: Token,
    val state: FormatState,
) {
    var newlines = current.start.line - previous.end.line
    var spaces = if (newlines == 0) current.start.column - previous.end.column else current.start.column - 1

    val isSameLine: Boolean get() = newlines == 0

    fun breakLine(lines: Int) {
        if (newlines == 0) spaces = state.lineIndent
        newlines = lines
    }
}

// lo que el formatter sabe del recorrido hasta aca. Es contexto general, no de una regla en particular:
// una regla nueva lo lee pero no necesita agregarle campos
class FormatState internal constructor() {
    var previous: Token? = null
        private set
    var depth = 0
        internal set
    var lineIndent = 0
        internal set

    // primer token de la sentencia en curso (let, println, if, un identificador...)
    var statementStart: Token? = null
        private set

    internal fun moveTo(token: Token) {
        val previousType = previous?.type
        if (previousType == null || previousType in STATEMENT_BOUNDARIES) statementStart = token
        if (token.type == TokenType.LEFTBRACE) depth++
        previous = token
    }
}
