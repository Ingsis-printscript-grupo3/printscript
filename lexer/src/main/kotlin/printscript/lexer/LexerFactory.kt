package printscript.lexer

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

// A diferencia de las otras fabricas esta no recibe LanguageVersion, y es a proposito:
// LexerRules.keywords es un superconjunto de las dos versiones y quien decide que token
// vale en cada una es el parser (VersionFeatures). Si el lexer recortara las keywords en
// 1.0, un `const` saldria como IDENTIFIER y el error dejaria de decir que la feature
// pertenece a 1.1.
object LexerFactory {
    fun create(
        charStream: CharStream,
        readers: List<TokenReader> = defaultReaders(),
    ): LexerInterface = Lexer(charStream, readers)

    fun create(
        reader: Reader,
        readers: List<TokenReader> = defaultReaders(),
    ): LexerInterface = create(CharStream(reader), readers)

    fun create(
        source: String,
        readers: List<TokenReader> = defaultReaders(),
    ): LexerInterface = create(StringReader(source), readers)

    // CharStream ya buferea el reader, asi que alcanza con envolver el stream
    fun create(
        input: InputStream,
        charset: Charset = StandardCharsets.UTF_8,
        readers: List<TokenReader> = defaultReaders(),
    ): LexerInterface = create(InputStreamReader(input, charset), readers)

    fun defaultReaders(): List<TokenReader> =
        listOf(
            IdentifierReader(LexerRules.keywords),
            NumberReader(),
            StringLiteralReader(),
            SymbolReader(LexerRules.symbols),
        )
}
