package printscript.semantic

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerFactory
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import printscript.semantic.handler.expression.BinaryExpressionHandler
import printscript.semantic.handler.expression.BooleanLiteralHandler
import printscript.semantic.handler.expression.IdentifierHandler
import printscript.semantic.handler.expression.NumberLiteralHandler
import printscript.semantic.handler.expression.ReadEnvHandler
import printscript.semantic.handler.expression.ReadInputHandler
import printscript.semantic.handler.expression.StringLiteralHandler
import printscript.semantic.handler.statement.AssignmentHandler
import printscript.semantic.handler.statement.BlockHandler
import printscript.semantic.handler.statement.IfStatementHandler
import printscript.semantic.handler.statement.PrintCallHandler
import printscript.semantic.handler.statement.VariableDeclarationHandler
import java.io.StringReader

// el semantico no sabe de versiones: los tests le pasan todos los handlers. Que trae cada version se prueba en runner
internal val STATEMENT_HANDLERS: List<SemanticStatementHandler> =
    listOf(
        VariableDeclarationHandler(),
        AssignmentHandler(),
        PrintCallHandler(),
        IfStatementHandler(),
        BlockHandler(),
    )

internal val EXPRESSION_HANDLERS: List<SemanticExpressionHandler> =
    listOf(
        NumberLiteralHandler(),
        StringLiteralHandler(),
        BooleanLiteralHandler(),
        IdentifierHandler(),
        BinaryExpressionHandler(),
        ReadInputHandler(),
        ReadEnvHandler(),
    )

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
): List<SemanticResult<Statement>> =
    SemanticAnalyzer(STATEMENT_HANDLERS, EXPRESSION_HANDLERS).analyze(parse(code, version)).asSequence().toList()
