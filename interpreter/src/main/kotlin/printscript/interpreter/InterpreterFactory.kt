package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.common.LanguageVersion
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.env.SystemEnvProvider
import printscript.interpreter.input.ConsoleInput
import printscript.interpreter.input.InputProvider
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
        input: InputProvider = ConsoleInput(),
        env: EnvProvider = SystemEnvProvider(),
    ): Interpreter =
        when (version) {
            LanguageVersion.V1_0 -> create10(output)
            LanguageVersion.V1_1 -> create11(output, input, env)
        }

    fun create(
        version: String,
        output: Output = ConsoleOutput(),
        input: InputProvider = ConsoleInput(),
        env: EnvProvider = SystemEnvProvider(),
    ): Interpreter = create(LanguageVersion.parse(version), output, input, env)

    fun create10(output: Output = ConsoleOutput()): Interpreter = Interpreter(LanguageVersion.V1_0, output)

    fun create11(
        output: Output = ConsoleOutput(),
        input: InputProvider = ConsoleInput(),
        env: EnvProvider = SystemEnvProvider(),
    ): Interpreter =
        Interpreter(
            statementInterpreters = default11StatementInterpreters(output),
            expressionEvaluators = default11ExpressionEvaluators(input, env),
        )

    fun default10StatementInterpreters(output: Output): List<Handler<Statement, InterpreterContext, Unit>> =
        listOf(
            VariableDeclarationInterpreter(),
            AssignmentInterpreter(),
            PrintCallInterpreter(output),
        )

    fun default11StatementInterpreters(output: Output): List<Handler<Statement, InterpreterContext, Unit>> =
        default10StatementInterpreters(output) +
            listOf(
                IfStatementInterpreter(),
                BlockInterpreter(),
            )

    fun default10ExpressionEvaluators(): List<Handler<Expression, InterpreterContext, Value>> =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            IdentifierEvaluator(),
            BinaryExpressionEvaluator(),
        )

    fun default11ExpressionEvaluators(
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
