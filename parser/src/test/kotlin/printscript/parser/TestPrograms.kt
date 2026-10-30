package printscript.parser

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerFactory
import printscript.parser.result.ParseResult
import java.io.StringReader

internal fun parseResults(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_1,
): List<ParseResult> {
    val tokens = Lexer(CharStream(StringReader(code)), LexerFactory.defaultReaders(version)).tokenize()
    return Parser(tokens, version).parse().asSequence().toList()
}

internal fun parseStatements(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_1,
): List<Statement> =
    parseResults(code, version).map { result ->
        when (result) {
            is ParseResult.Success -> result.statement
            is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
        }
    }
