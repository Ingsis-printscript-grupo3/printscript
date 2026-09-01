package printscript.lexer

import printscript.common.TokenType
import java.io.Reader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Evidencia de que el lexer consume el fuente de a poco y no de una sola vez.
 *
 * El instrumento es un [CountingReader]: envuelve al `Reader` real y cuenta cuantos
 * caracteres se le pidieron efectivamente. Si el lexer materializara el fuente, el
 * contador quedaria en el total antes de emitir el primer token.
 */
class LexerStreamingTest {
    private class CountingReader(private val delegate: Reader) : Reader() {
        var charsRead: Int = 0
            private set

        override fun read(
            cbuf: CharArray,
            off: Int,
            len: Int,
        ): Int {
            val read = delegate.read(cbuf, off, len)
            if (read > 0) charsRead += read
            return read
        }

        override fun close() {
            delegate.close()
        }
    }

    @Test
    fun `CharStream bufferiza internamente aunque le pasen un Reader sin buffer`() {
        val source = STATEMENT.repeat(REPETITIONS_SMALL)
        val counting = CountingReader(StringReader(source))

        // construir el CharStream ya lee current y next
        CharStream(counting)

        // sin buffering interno serian exactamente 2 caracteres: un read() por cada uno
        assertTrue(
            counting.charsRead > 2,
            "CharStream leyo ${counting.charsRead} caracteres: no hay buffering interno",
        )
    }

    @Test
    fun `pedir un solo token lee una fraccion del fuente`() {
        val source = STATEMENT.repeat(REPETITIONS_LARGE)
        val counting = CountingReader(StringReader(source))

        val firstToken = Lexer(CharStream(counting)).tokenize().next()

        assertEquals(TokenType.LET, firstToken.type)
        assertTrue(
            counting.charsRead < source.length / FRACTION_DIVISOR,
            "para el primer token leyo ${counting.charsRead} de ${source.length} caracteres",
        )
    }

    @Test
    fun `consumir el iterador completo si lee todo el fuente`() {
        val source = STATEMENT.repeat(REPETITIONS_LARGE)
        val counting = CountingReader(StringReader(source))

        val tokens = Lexer(CharStream(counting)).tokenize().asSequence().count()

        assertTrue(tokens > REPETITIONS_LARGE)
        assertEquals(source.length, counting.charsRead)
    }

    @Test
    fun `una linea mas larga que cualquier buffer se lexea igual`() {
        // si el lexer leyera por lineas, esta sola linea seria todo el fuente en memoria
        val content = "x".repeat(LONG_LINE_CHARS)
        val source = "let largo: string = \"$content\";"

        val tokens = Lexer(CharStream(StringReader(source))).tokenize().asSequence().toList()

        val literal = tokens.single { it.type == TokenType.STRINGLITERAL }
        assertEquals(LONG_LINE_CHARS, literal.value.length)
        assertEquals(TokenType.EOF, tokens.last().type)
    }

    private companion object {
        const val STATEMENT = "let variable: number = 12345;\n"
        const val REPETITIONS_SMALL = 3
        const val REPETITIONS_LARGE = 10_000
        const val FRACTION_DIVISOR = 10
        const val LONG_LINE_CHARS = 100_000
    }
}
