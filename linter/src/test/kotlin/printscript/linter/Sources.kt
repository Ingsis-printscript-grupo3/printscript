package printscript.linter

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.lexer.CharStream
import printscript.lexer.LexerFactory
import printscript.parser.ParserFactory
import printscript.parser.result.ParseResult
import java.io.StringReader

// el AST sale del lexer y el parser reales, no armado a mano
fun statementsOf(
    source: String,
    version: LanguageVersion = LanguageVersion.V1_1,
): List<Statement> {
    val tokens = LexerFactory.create(CharStream(StringReader(source)), version).tokenize()
    return ParserFactory.create(tokens, version).parse().asSequence().map { result ->
        when (result) {
            is ParseResult.Success -> result.statement
            is ParseResult.Failure -> error("no deberia fallar el parseo: ${result.message} en ${result.start}")
        }
    }.toList()
}

fun warningsOf(
    linter: Linter,
    source: String,
    version: LanguageVersion = LanguageVersion.V1_1,
): List<Warning> {
    val warnings = mutableListOf<Warning>()
    linter.analyze(statementsOf(source, version).iterator(), warnings::add)
    return warnings
}
