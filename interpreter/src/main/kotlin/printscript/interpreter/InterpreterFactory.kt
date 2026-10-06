package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.common.LanguageVersion
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.InputProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.ConsoleOutput
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.InterpreterContext
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

object InterpreterFactory {
    fun create(
        version: LanguageVersion,
        output: Output = ConsoleOutput(),
        input: InputProvider = QueueInput(),
        env: EnvProvider = MapEnvProvider(),
    ): Interpreter =
        when (version) {
            // input y env se descartan a proposito: 1.0 no tiene readInput ni readEnv
            LanguageVersion.V1_0 -> create10(output)
            LanguageVersion.V1_1 -> create11(output, input, env)
        }

    fun create(
        version: String,
        output: Output = ConsoleOutput(),
        input: InputProvider = QueueInput(),
        env: EnvProvider = MapEnvProvider(),
    ): Interpreter = create(LanguageVersion.parse(version), output, input, env)

    // el interprete decide por version QUE handlers registra: si le llega un nodo que su version
    // no conoce, corta con UnknownStatementError. El semantico hace lo mismo y ademas guarda en datos
    // lo que cambia dentro de un mismo nodo (ver SemanticRules)
    fun create10(output: Output = ConsoleOutput()): Interpreter =
        Interpreter(
            statementInterpreters = default10StatementInterpreters(output),
            expressionEvaluators = default10ExpressionEvaluators(),
        )

    fun create11(
        output: Output = ConsoleOutput(),
        input: InputProvider = QueueInput(),
        env: EnvProvider = MapEnvProvider(),
    ): Interpreter =
        Interpreter(
            statementInterpreters = default11StatementInterpreters(output),
            expressionEvaluators = default11ExpressionEvaluators(input, env),
        )

    internal fun default10StatementInterpreters(output: Output): List<Handler<Statement, InterpreterContext, Unit>> =
        listOf(
            VariableDeclarationInterpreter(),
            AssignmentInterpreter(),
            PrintCallInterpreter(output),
        )

    internal fun default11StatementInterpreters(output: Output): List<Handler<Statement, InterpreterContext, Unit>> =
        default10StatementInterpreters(output) +
            listOf(
                IfStatementInterpreter(),
                BlockInterpreter(),
            )

    internal fun default10ExpressionEvaluators(): List<Handler<Expression, InterpreterContext, Value>> =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            IdentifierEvaluator(),
            BinaryExpressionEvaluator(),
        )

    internal fun default11ExpressionEvaluators(
        input: InputProvider,
        env: EnvProvider,
    ): List<Handler<Expression, InterpreterContext, Value>> =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            BooleanLiteralEvaluator(),
            IdentifierEvaluator(),
            BinaryExpressionEvaluator(),
            ReadInputEvaluator(input),
            ReadEnvEvaluator(env),
        )
}
