package printscript.interpreter

import printscript.ast.Statement
import printscript.common.LanguageVersion
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
import printscript.parser.result.ParseResult
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

// LanguageVersion solo esta para armar el fixture: el parser todavia la pide
internal fun parse(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_0,
): Iterator<Statement> {
    val tokens = Lexer(CharStream(StringReader(code)), LexerFactory.defaultReaders(version.keywords)).tokenize()
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
