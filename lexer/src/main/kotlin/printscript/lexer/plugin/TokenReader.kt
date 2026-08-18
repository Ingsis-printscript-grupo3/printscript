package printscript.lexer.plugin

import printscript.common.Position
import printscript.common.Token
import printscript.lexer.CharStream

interface TokenReader {

    fun matches(char: Char): Boolean

    fun read(stream: CharStream, start: Position): Token
}
