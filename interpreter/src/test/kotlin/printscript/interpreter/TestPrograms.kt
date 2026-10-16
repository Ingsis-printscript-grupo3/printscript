package printscript.interpreter

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerFactory
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import java.io.StringReader

// parsea con 1.1 para que los tests de 1.0 reciban nodos que su interprete no soporta
internal fun parse(code: String): Iterator<Statement> {
    val version = LanguageVersion.V1_1
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
