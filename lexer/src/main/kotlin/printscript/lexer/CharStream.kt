package printscript.lexer

import printscript.common.Position
import java.io.Reader

/**
 * Recorre el fuente caracter por caracter, con un lookahead de uno, sin materializarlo.
 *
 * El buffering es responsabilidad de esta clase, no de quien la construye: cualquier
 * `Reader` alcanza para armar un `CharStream` eficiente. Por eso el `reader.buffered()`
 * se declara ANTES de `current` y `next`: los inicializadores de propiedades corren en
 * orden de declaracion, y si `current` quedara arriba leeria del `Reader` crudo (un
 * syscall por caracter).
 *
 * No se lee por lineas a proposito: una linea puede ser arbitrariamente larga, asi que
 * `readLine()` volveria a materializar una parte no acotada del fuente.
 */
class CharStream(reader: Reader) {
    private val reader = reader.buffered()
    private var current: Int = this.reader.read()
    private var next: Int = this.reader.read()

    private var line: Int = 1
    private var column: Int = 1

    fun isAtEnd(): Boolean = current == -1

    fun peek(): Char? = if (current == -1) null else current.toChar()

    fun peekNext(): Char? = if (next == -1) null else next.toChar()

    fun position(): Position = Position(line, column)

    fun advance(): Char {
        check(current != -1) { "Cannot advance past end of stream" }

        val consumed = current.toChar()

        if (consumed == '\n') {
            line++
            column = 1
        } else {
            column++
        }

        current = next
        next = reader.read()

        return consumed
    }
}
