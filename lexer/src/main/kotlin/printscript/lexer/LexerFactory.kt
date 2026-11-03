package printscript.lexer

import printscript.common.TokenType
import printscript.lexer.plugin.TokenReader
import printscript.lexer.plugin.reader.IdentifierReader
import printscript.lexer.plugin.reader.NumberReader
import printscript.lexer.plugin.reader.StringLiteralReader
import printscript.lexer.plugin.reader.SymbolReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

// las keywords llegan de afuera: que palabras reserva cada version lo decide quien arma el
// pipeline, no el lexer. Con un mapa sin `const` ni `if`, esas palabras son identificadores comunes
object LexerFactory {
    fun create(
        charStream: CharStream,
        keywords: Map<String, TokenType>,
        readers: List<TokenReader> = defaultReaders(keywords),
    ): Lexer = Lexer(charStream, readers)

    fun create(
        reader: Reader,
        keywords: Map<String, TokenType>,
        readers: List<TokenReader> = defaultReaders(keywords),
    ): Lexer = create(CharStream(reader), keywords, readers)

    fun create(
        source: String,
        keywords: Map<String, TokenType>,
        readers: List<TokenReader> = defaultReaders(keywords),
    ): Lexer = create(StringReader(source), keywords, readers)

    // CharStream ya buferea el reader, asi que alcanza con envolver el stream
    fun create(
        input: InputStream,
        keywords: Map<String, TokenType>,
        charset: Charset = StandardCharsets.UTF_8,
        readers: List<TokenReader> = defaultReaders(keywords),
    ): Lexer = create(InputStreamReader(input, charset), keywords, readers)

    fun defaultReaders(keywords: Map<String, TokenType>): List<TokenReader> =
        listOf(
            IdentifierReader(keywords),
            NumberReader(),
            StringLiteralReader(),
            SymbolReader(LexerRules.symbols),
        )
}
