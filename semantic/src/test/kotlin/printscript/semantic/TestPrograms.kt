package printscript.semantic

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerFactory
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import java.io.StringReader

internal fun parse(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_0,
): Iterator<Statement> {
    val tokens = Lexer(CharStream(StringReader(code)), LexerFactory.defaultReaders(version)).tokenize()
    return Parser(tokens, version)
        .parse()
        .asSequence()
        .map { result ->
            when (result) {
                is ParseResult.Success -> result.statement
                is ParseResult.Failure -> error("el codigo del test no parsea: ${result.message} en ${result.start}")
            }
        }.iterator()
}

internal fun analyze(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_0,
): List<SemanticResult<Statement>> = SemanticAnalyzer(version).analyze(parse(code, version)).asSequence().toList()
