package printscript.lexer

import printscript.common.LanguageVersion
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

// cada version arma sus readers con sus propias keywords: en 1.0 `const` o `if` son
// identificadores comunes
object LexerFactory {
    fun create(
        charStream: CharStream,
        version: LanguageVersion,
        readers: List<TokenReader> = defaultReaders(version),
    ): LexerInterface = Lexer(charStream, readers)

    fun create(
        reader: Reader,
        version: LanguageVersion,
        readers: List<TokenReader> = defaultReaders(version),
    ): LexerInterface = create(CharStream(reader), version, readers)

    fun create(
        source: String,
        version: LanguageVersion,
        readers: List<TokenReader> = defaultReaders(version),
    ): LexerInterface = create(StringReader(source), version, readers)

    // CharStream ya buferea el reader, asi que alcanza con envolver el stream
    fun create(
        input: InputStream,
        version: LanguageVersion,
        charset: Charset = StandardCharsets.UTF_8,
        readers: List<TokenReader> = defaultReaders(version),
    ): LexerInterface = create(InputStreamReader(input, charset), version, readers)

    fun defaultReaders(version: LanguageVersion): List<TokenReader> =
        listOf(
            IdentifierReader(LexerRules.keywordsFor(version)),
            NumberReader(),
            StringLiteralReader(),
            SymbolReader(LexerRules.symbols),
        )
}
