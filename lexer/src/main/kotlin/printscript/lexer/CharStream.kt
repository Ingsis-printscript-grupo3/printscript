package printscript.lexer

import printscript.common.Position
import java.io.Reader

class CharStream(private val reader: Reader) {
    private var current: Int = reader.read()
    private var next: Int = reader.read()

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
