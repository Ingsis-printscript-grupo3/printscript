package printscript.lexer

import printscript.common.Token
import printscript.common.TokenType
import printscript.lexer.plugin.TokenReader

class Lexer(
    private val charStream: CharStream,
    private val readers: List<TokenReader>,
) : LexerInterface {
    // constructor con los readers de PrintScript para q los tests y el CLI puedan
    // seguir creando el Lexer con un solo argumento. La lista vive en LexerFactory
    // para no tener dos copias del mismo default
    constructor(charStream: CharStream) : this(charStream, LexerFactory.defaultReaders())

    override fun tokenize(): Iterator<Token> =
        iterator {
            while (true) {
                val token = nextToken()
                yield(token)
                if (token.type == TokenType.EOF) break
            }
        }

    private fun nextToken(): Token {
        skipWhitespace()

        val start = charStream.position()

        if (charStream.isAtEnd()) {
            return Token(TokenType.EOF, start, start, "")
        }

        val char = charStream.peek()!!

        val reader =
            readers.firstOrNull { it.matches(char) }
                ?: run {
                    charStream.advance()
                    throw LexicalError("Unexpected character: '$char'", start, charStream.position())
                }

        return reader.read(charStream, start)
    }

    private fun skipWhitespace() {
        while (!charStream.isAtEnd() && isWhitespace(charStream.peek()!!)) {
            charStream.advance()
        }
    }

    private fun isWhitespace(char: Char): Boolean = char == ' ' || char == '\t' || char == '\r' || char == '\n'
}
