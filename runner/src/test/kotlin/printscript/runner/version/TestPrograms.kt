package printscript.runner.version

import printscript.ast.Statement
import printscript.lexer.LexerFactory
import printscript.parser.ParserFactory
import printscript.parser.result.ParseResult
import kotlin.test.assertIs

// se parsea con 1.1 para que el semantico o el interprete de 1.0 reciban nodos que no soportan
internal fun parse11(code: String): Iterator<Statement> {
    val tokens = LexerFactory.create(code, PrintScript11.keywords).tokenize()
    return ParserFactory
        .create(tokens, PrintScript11.prefixParselets, PrintScript11.parserStatements, PrintScript11.infixParselets)
        .parse()
        .asSequence()
        .map { result -> assertIs<ParseResult.Success>(result).statement }
        .iterator()
}
