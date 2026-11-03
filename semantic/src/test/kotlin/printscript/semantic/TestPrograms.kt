package printscript.semantic

import printscript.ast.Statement
import printscript.common.LanguageVersion
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
import printscript.parser.statement.handlers.AssignmentHandler
import printscript.parser.statement.handlers.IfStatementHandler
import printscript.parser.statement.handlers.PrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler
import java.io.StringReader

private val KEYWORDS_1_0 =
    mapOf(
        "let" to TokenType.LET,
        "println" to TokenType.PRINTLN,
        "number" to TokenType.NUMBERTYPE,
        "string" to TokenType.STRINGTYPE,
    )

private val KEYWORDS_1_1 =
    KEYWORDS_1_0 +
        mapOf(
            "const" to TokenType.CONST,
            "boolean" to TokenType.BOOLEANTYPE,
            "true" to TokenType.BOOLEANLITERAL,
            "false" to TokenType.BOOLEANLITERAL,
            "if" to TokenType.IF,
            "else" to TokenType.ELSE,
            "readInput" to TokenType.READINPUT,
            "readEnv" to TokenType.READENV,
        )

private val PREFIX_1_0: Map<TokenType, PrefixParselet> =
    mapOf(
        TokenType.NUMBERLITERAL to NumberLiteralParselet,
        TokenType.STRINGLITERAL to StringLiteralParselet,
        TokenType.IDENTIFIER to IdentifierParselet,
        TokenType.LEFTPAREN to ParenthesizedExpressionParselet,
        TokenType.MINUS to UnaryMinusParselet(),
    )

private val PREFIX_1_1: Map<TokenType, PrefixParselet> =
    PREFIX_1_0 +
        mapOf(
            TokenType.BOOLEANLITERAL to BooleanLiteralParselet,
            TokenType.READINPUT to ReadInputParselet,
            TokenType.READENV to ReadEnvParselet,
        )

private val TYPES_1_0 = setOf(TokenType.NUMBERTYPE, TokenType.STRINGTYPE)
private val TYPES_1_1 = TYPES_1_0 + TokenType.BOOLEANTYPE

private val HANDLERS_1_0: Map<TokenType, StatementHandler> =
    mapOf(
        TokenType.LET to VariableDeclarationHandler(TYPES_1_0),
        TokenType.PRINTLN to PrintCallHandler,
        TokenType.IDENTIFIER to AssignmentHandler,
    )

private val HANDLERS_1_1: Map<TokenType, StatementHandler> =
    HANDLERS_1_0 +
        mapOf(
            TokenType.LET to VariableDeclarationHandler(TYPES_1_1),
            TokenType.CONST to VariableDeclarationHandler(TYPES_1_1),
            TokenType.IF to IfStatementHandler,
        )

internal fun parse(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_0,
): Iterator<Statement> {
    val keywords = if (version == LanguageVersion.V1_0) KEYWORDS_1_0 else KEYWORDS_1_1
    val prefix = if (version == LanguageVersion.V1_0) PREFIX_1_0 else PREFIX_1_1
    val handlers = if (version == LanguageVersion.V1_0) HANDLERS_1_0 else HANDLERS_1_1
    val tokens = Lexer(CharStream(StringReader(code)), LexerFactory.defaultReaders(keywords)).tokenize()
    return Parser(tokens, prefix, handlers)
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
