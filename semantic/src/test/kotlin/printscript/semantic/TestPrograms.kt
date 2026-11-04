package printscript.semantic

import printscript.ast.Statement
import printscript.common.TokenType
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerFactory
import printscript.parser.Parser
import printscript.parser.expression.BooleanLiteralParselet
import printscript.parser.expression.IdentifierParselet
import printscript.parser.expression.NumberLiteralParselet
import printscript.parser.expression.ParenthesizedExpressionParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.expression.ReadEnvParselet
import printscript.parser.expression.ReadInputParselet
import printscript.parser.expression.StringLiteralParselet
import printscript.parser.expression.UnaryMinusParselet
import printscript.parser.result.ParseResult
import printscript.parser.statement.StatementHandler
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
import printscript.parser.statement.handlers.AssignmentHandler as ParserAssignmentHandler
import printscript.parser.statement.handlers.IfStatementHandler as ParserIfStatementHandler
import printscript.parser.statement.handlers.PrintCallHandler as ParserPrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler as ParserVariableDeclarationHandler

// el semantico no sabe de versiones: los tests le pasan todos los handlers
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

// el parser aca solo actua como fixture generator para armar el ast de prueba
private val KEYWORDS: Map<String, TokenType> =
    mapOf(
        "let" to TokenType.LET,
        "println" to TokenType.PRINTLN,
        "number" to TokenType.NUMBERTYPE,
        "string" to TokenType.STRINGTYPE,
        "const" to TokenType.CONST,
        "boolean" to TokenType.BOOLEANTYPE,
        "true" to TokenType.BOOLEANLITERAL,
        "false" to TokenType.BOOLEANLITERAL,
        "if" to TokenType.IF,
        "else" to TokenType.ELSE,
        "readInput" to TokenType.READINPUT,
        "readEnv" to TokenType.READENV,
    )

private val PREFIX_PARSELETS: Map<TokenType, PrefixParselet> =
    mapOf(
        TokenType.NUMBERLITERAL to NumberLiteralParselet,
        TokenType.STRINGLITERAL to StringLiteralParselet,
        TokenType.IDENTIFIER to IdentifierParselet,
        TokenType.LEFTPAREN to ParenthesizedExpressionParselet,
        TokenType.MINUS to UnaryMinusParselet(),
        TokenType.BOOLEANLITERAL to BooleanLiteralParselet,
        TokenType.READINPUT to ReadInputParselet,
        TokenType.READENV to ReadEnvParselet,
    )

private val TYPES = setOf(TokenType.NUMBERTYPE, TokenType.STRINGTYPE, TokenType.BOOLEANTYPE)

private val HANDLERS: Map<TokenType, StatementHandler> =
    mapOf(
        TokenType.LET to ParserVariableDeclarationHandler(TYPES),
        TokenType.PRINTLN to ParserPrintCallHandler,
        TokenType.IDENTIFIER to ParserAssignmentHandler,
        TokenType.CONST to ParserVariableDeclarationHandler(TYPES),
        TokenType.IF to ParserIfStatementHandler,
    )

internal fun parse(code: String): Iterator<Statement> {
    val tokens = Lexer(CharStream(StringReader(code)), LexerFactory.defaultReaders(KEYWORDS)).tokenize()
    return Parser(tokens, PREFIX_PARSELETS, HANDLERS)
        .parse()
        .asSequence()
        .map { result ->
            when (result) {
                is ParseResult.Success -> result.statement
                is ParseResult.Failure -> error("el codigo del test no parsea: ${result.message} en ${result.start}")
            }
        }.iterator()
}

internal fun analyze(code: String): List<SemanticResult<Statement>> =
    SemanticAnalyzer(STATEMENT_HANDLERS, EXPRESSION_HANDLERS).analyze(parse(code)).asSequence().toList()
