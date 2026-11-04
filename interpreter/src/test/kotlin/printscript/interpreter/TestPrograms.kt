package printscript.interpreter

import printscript.ast.Statement
import printscript.common.TokenType
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.InputProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.BooleanLiteralEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.ReadEnvEvaluator
import printscript.interpreter.plugin.expression.ReadInputEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.BlockInterpreter
import printscript.interpreter.plugin.statement.IfStatementInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter
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

// el interprete no sabe de versiones: los tests le pasan todos los handlers. Que trae cada version se prueba en runner
internal fun testInterpreter(
    output: Output,
    input: InputProvider = QueueInput(),
    env: EnvProvider = MapEnvProvider(),
): Interpreter =
    Interpreter(
        statementInterpreters =
            listOf(
                VariableDeclarationInterpreter(),
                AssignmentInterpreter(),
                PrintCallInterpreter(output),
                IfStatementInterpreter(),
                BlockInterpreter(),
            ),
        expressionEvaluators =
            listOf(
                NumberLiteralEvaluator(),
                StringLiteralEvaluator(),
                BooleanLiteralEvaluator(),
                IdentifierEvaluator(),
                BinaryExpressionEvaluator(),
                ReadInputEvaluator(input),
                ReadEnvEvaluator(env),
            ),
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
        TokenType.LET to VariableDeclarationHandler(TYPES),
        TokenType.PRINTLN to PrintCallHandler,
        TokenType.IDENTIFIER to AssignmentHandler,
        TokenType.CONST to VariableDeclarationHandler(TYPES),
        TokenType.IF to IfStatementHandler,
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
