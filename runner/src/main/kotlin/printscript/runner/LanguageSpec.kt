package printscript.runner

import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.parser.expression.BooleanLiteralParselet
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.expression.IdentifierParselet
import printscript.parser.expression.InfixParselet
import printscript.parser.expression.NumberLiteralParselet
import printscript.parser.expression.ParenthesizedExpressionParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.expression.ReadEnvParselet
import printscript.parser.expression.ReadInputParselet
import printscript.parser.expression.StringLiteralParselet
import printscript.parser.expression.UnaryMinusParselet
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.handlers.AssignmentHandler
import printscript.parser.statement.handlers.IfStatementHandler
import printscript.parser.statement.handlers.PrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler

// la tabla que define que trae cada version del lenguaje vive en :runner, porque es el
// modulo que integra todas las piezas. los modulos del nucleo no conocen versiones
internal data class LanguageSpec(
    val keywords: Map<String, TokenType>,
    val prefixParselets: Map<TokenType, PrefixParselet>,
    val statementHandlers: Map<TokenType, StatementHandler>,
    val infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix,
)

private val KEYWORDS_1_0: Map<String, TokenType> =
    mapOf(
        "let" to TokenType.LET,
        "println" to TokenType.PRINTLN,
        "number" to TokenType.NUMBERTYPE,
        "string" to TokenType.STRINGTYPE,
    )

// palabras clave explicitas para 1.1: en 1.0 las adicionales se tratan como identificadores
private val KEYWORDS_1_1: Map<String, TokenType> =
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

private val unaryMinus = UnaryMinusParselet()

private val PREFIX_1_0: Map<TokenType, PrefixParselet> =
    mapOf(
        TokenType.NUMBERLITERAL to NumberLiteralParselet,
        TokenType.STRINGLITERAL to StringLiteralParselet,
        TokenType.IDENTIFIER to IdentifierParselet,
        TokenType.LEFTPAREN to ParenthesizedExpressionParselet,
        TokenType.MINUS to unaryMinus,
    )

// parselets de prefijo explicitos para 1.1
private val PREFIX_1_1: Map<TokenType, PrefixParselet> =
    mapOf(
        TokenType.NUMBERLITERAL to NumberLiteralParselet,
        TokenType.STRINGLITERAL to StringLiteralParselet,
        TokenType.IDENTIFIER to IdentifierParselet,
        TokenType.LEFTPAREN to ParenthesizedExpressionParselet,
        TokenType.MINUS to unaryMinus,
        TokenType.BOOLEANLITERAL to BooleanLiteralParselet,
        TokenType.READINPUT to ReadInputParselet,
        TokenType.READENV to ReadEnvParselet,
    )

private val TYPES_1_0 = setOf(TokenType.NUMBERTYPE, TokenType.STRINGTYPE)
private val TYPES_1_1 = setOf(TokenType.NUMBERTYPE, TokenType.STRINGTYPE, TokenType.BOOLEANTYPE)

private val HANDLERS_1_0: Map<TokenType, StatementHandler> =
    mapOf(
        TokenType.LET to VariableDeclarationHandler(TYPES_1_0),
        TokenType.PRINTLN to PrintCallHandler,
        TokenType.IDENTIFIER to AssignmentHandler,
    )

// manejadores de sentencias explicitos para 1.1
private val HANDLERS_1_1: Map<TokenType, StatementHandler> =
    mapOf(
        TokenType.LET to VariableDeclarationHandler(TYPES_1_1),
        TokenType.PRINTLN to PrintCallHandler,
        TokenType.IDENTIFIER to AssignmentHandler,
        TokenType.CONST to VariableDeclarationHandler(TYPES_1_1),
        TokenType.IF to IfStatementHandler,
    )

private val SPEC_1_0 =
    LanguageSpec(
        keywords = KEYWORDS_1_0,
        prefixParselets = PREFIX_1_0,
        statementHandlers = HANDLERS_1_0,
    )

private val SPEC_1_1 =
    LanguageSpec(
        keywords = KEYWORDS_1_1,
        prefixParselets = PREFIX_1_1,
        statementHandlers = HANDLERS_1_1,
    )

internal fun languageSpecFor(version: LanguageVersion): LanguageSpec =
    when (version) {
        LanguageVersion.V1_0 -> SPEC_1_0
        LanguageVersion.V1_1 -> SPEC_1_1
    }
