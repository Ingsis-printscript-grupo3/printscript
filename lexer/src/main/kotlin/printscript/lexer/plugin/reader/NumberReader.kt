package printscript.lexer.plugin.reader

import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.lexer.CharStream
import printscript.lexer.plugin.TokenReader

class NumberReader : TokenReader {

    override fun matches(char: Char): Boolean = char.isDigit()

    override fun read(stream: CharStream, start: Position): Token {
        val text = StringBuilder()
        while (!stream.isAtEnd() && stream.peek()!!.isDigit()) {
            text.append(stream.advance())
        }

        if (!stream.isAtEnd() && stream.peek() == '.' && stream.peekNext()?.isDigit() == true) {
            text.append(stream.advance())
            while (!stream.isAtEnd() && stream.peek()!!.isDigit()) {
                text.append(stream.advance())
            }
        }

        return Token(TokenType.NUMBERLITERAL, start, stream.position(), text.toString())
    }
}
