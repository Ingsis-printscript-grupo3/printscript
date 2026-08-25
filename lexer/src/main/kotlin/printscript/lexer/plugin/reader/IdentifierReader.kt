package printscript.lexer.plugin.reader

import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.lexer.CharStream
import printscript.lexer.LexerRules
import printscript.lexer.plugin.TokenReader

class IdentifierReader(private val keywords: Map<String, TokenType>) : TokenReader {
    override fun matches(char: Char): Boolean = LexerRules.isIdentifierStart(char)

    override fun read(
        stream: CharStream,
        start: Position,
    ): Token {
        val text = StringBuilder()
        while (!stream.isAtEnd() && LexerRules.isIdentifierPart(stream.peek()!!)) {
            text.append(stream.advance())
        }

        val end = stream.position()
        val value = text.toString()
        val type = keywords[value] ?: TokenType.IDENTIFIER

        return Token(type, start, end, value)
    }
}
