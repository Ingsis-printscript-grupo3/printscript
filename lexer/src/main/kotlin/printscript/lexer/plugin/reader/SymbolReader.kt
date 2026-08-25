package printscript.lexer.plugin.reader

import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.lexer.CharStream
import printscript.lexer.plugin.TokenReader

class SymbolReader(private val symbols: Map<Char, TokenType>) : TokenReader {
    override fun matches(char: Char): Boolean = symbols.containsKey(char)

    override fun read(
        stream: CharStream,
        start: Position,
    ): Token {
        val char = stream.advance()
        return Token(symbols.getValue(char), start, stream.position(), char.toString())
    }
}
