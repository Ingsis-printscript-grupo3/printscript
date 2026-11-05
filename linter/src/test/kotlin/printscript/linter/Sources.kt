package printscript.linter

import printscript.ast.Expression
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.common.TokenType
import printscript.lexer.CharStream
import printscript.lexer.LexerFactory
import printscript.linter.rule.LinterRule
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
        TokenType.LET to VariableDeclarationHandler(TYPES),
        TokenType.PRINTLN to PrintCallHandler,
        TokenType.IDENTIFIER to AssignmentHandler,
        TokenType.CONST to VariableDeclarationHandler(TYPES),
        TokenType.IF to IfStatementHandler,
    )

// el ast sale del lexer y el parser reales, no armado a mano
fun statementsOf(source: String): List<Statement> {
    val tokens = LexerFactory.create(CharStream(StringReader(source)), KEYWORDS).tokenize()
    return Parser(tokens, PREFIX_PARSELETS, HANDLERS).parse().asSequence().map { result ->
        when (result) {
            is ParseResult.Success -> result.statement
            is ParseResult.Failure -> error("no deberia fallar el parseo: ${result.message} en ${result.start}")
        }
    }.toList()
}

fun statementOf(source: String): Statement = statementsOf(source).single()

// el parser solo arma expresiones dentro de una sentencia, asi que se la saca de un println
fun expressionOf(source: String): Expression = (statementOf("println($source);") as PrintCall).value

fun warningsOf(
    linter: Linter,
    source: String,
): List<Warning> {
    val warnings = mutableListOf<Warning>()
    linter.analyze(statementsOf(source).iterator(), warnings::add)
    return warnings
}

// corre una sola regla, con el mismo recorrido del ast que usa el linter real
fun warningsOf(
    rule: LinterRule,
    source: String,
): List<Warning> = warningsOf(Linter(listOf(rule)), source)
