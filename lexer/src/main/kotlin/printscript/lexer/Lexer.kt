package printscript.lexer

import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType

class Lexer(private val charStream: CharStream) {

    fun tokenize(): Sequence<Token> = sequence {
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

        val c = charStream.peek()!!

        return when {
            LexerRules.isIdentifierStart(c) -> readIdentifier(start)
            c.isDigit() -> readNumber(start)
            LexerRules.isQuote(c) -> readString(start)
            LexerRules.symbols.containsKey(c) -> readSymbol(start, c)
            else -> {
                charStream.advance()
                throw LexicalError("Carácter inesperado: '$c'", start, charStream.position())
            }
        }
    }

    private fun skipWhitespace() {
        while (!charStream.isAtEnd() && charStream.peek()!!.let { it == ' ' || it == '\t' || it == '\r' || it == '\n' }) {
            charStream.advance()
        }
    }

    private fun readIdentifier(start: Position): Token {
        val text = StringBuilder()
        while (!charStream.isAtEnd() && LexerRules.isIdentifierPart(charStream.peek()!!)) {
            text.append(charStream.advance())
        }
        val end = charStream.position()
        val type = LexerRules.keywords[text.toString()] ?: TokenType.IDENTIFIER
        return Token(type, start, end, text.toString())
    }

    private fun readNumber(start: Position): Token {
        val text = StringBuilder()
        while (!charStream.isAtEnd() && charStream.peek()!!.isDigit()) {
            text.append(charStream.advance())
        }

        if (!charStream.isAtEnd() && charStream.peek() == '.' && charStream.peekNext()?.isDigit() == true) {
            text.append(charStream.advance())
            while (!charStream.isAtEnd() && charStream.peek()!!.isDigit()) {
                text.append(charStream.advance())
            }
        }

        val end = charStream.position()
        return Token(TokenType.NUMBERLITERAL, start, end, text.toString())
    }

    private fun readString(start: Position): Token {
        val quote = charStream.advance()
        val text = StringBuilder()

        while (true) {
            if (charStream.isAtEnd() || charStream.peek() == '\n') {
                throw LexicalError("String sin cerrar", start, charStream.position())
            }
            val c = charStream.advance()
            if (c == quote) break
            text.append(c)
        }

        val end = charStream.position()
        return Token(TokenType.STRINGLITERAL, start, end, text.toString())
    }

    private fun readSymbol(start: Position, c: Char): Token {
        charStream.advance()
        val end = charStream.position()
        val type = LexerRules.symbols.getValue(c)
        return Token(type, start, end, c.toString())
    }
}