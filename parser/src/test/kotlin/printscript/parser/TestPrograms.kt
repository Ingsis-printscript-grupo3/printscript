package printscript.parser

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerFactory
import printscript.parser.result.ParseResult
import java.io.StringReader

internal fun tokensOf(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_1,
): Iterator<Token> = Lexer(CharStream(StringReader(code)), LexerFactory.defaultReaders(version)).tokenize()

// lexerVersion permite que el parser reciba tokens que el lexer de su propia version no emite
internal fun parseResults(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_1,
    lexerVersion: LanguageVersion = version,
): List<ParseResult> = Parser(tokensOf(code, lexerVersion), version).parse().asSequence().toList()

internal fun parseStatements(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_1,
    lexerVersion: LanguageVersion = version,
): List<Statement> =
    parseResults(code, version, lexerVersion).map { result ->
        when (result) {
            is ParseResult.Success -> result.statement
            is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
        }
    }
