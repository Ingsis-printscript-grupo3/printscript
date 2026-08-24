package printscript.lexer.plugin.reader

import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.lexer.CharStream
import printscript.lexer.LexerRules
import printscript.lexer.LexicalError
import printscript.lexer.plugin.TokenReader

class StringLiteralReader : TokenReader {
    override fun matches(char: Char): Boolean = LexerRules.isQuote(char)

    override fun read(
        stream: CharStream,
        start: Position,
    ): Token {
        val quote = stream.advance()
        val text = StringBuilder()

        while (true) {
            if (stream.isAtEnd() || stream.peek() == '\n') {
                throw LexicalError("String sin cerrar", start, stream.position())
            }
            val char = stream.advance()
            if (char == quote) break
            text.append(char)
        }

        return Token(TokenType.STRINGLITERAL, start, stream.position(), text.toString())
    }
}
