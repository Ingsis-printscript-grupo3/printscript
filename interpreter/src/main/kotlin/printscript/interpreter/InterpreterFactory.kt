package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
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
import printscript.interpreter.plugin.statement.Assignment11Interpreter
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.BlockInterpreter
import printscript.interpreter.plugin.statement.ConstDeclarationInterpreter
import printscript.interpreter.plugin.statement.IfStatementInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclaration11Interpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter

object InterpreterFactory {
    fun create(
        version: String,
        output: Output = ConsoleOutput(),
        input: InputProvider = ConsoleInput(),
        env: EnvProvider = SystemEnvProvider(),
    ): Interpreter =
        when (version) {
            "1.0" -> create10(output)
            "1.1" -> create11(output, input, env)
            else -> throw IllegalArgumentException("Unsupported PrintScript version: $version")
        }

    fun create10(output: Output = ConsoleOutput()): Interpreter =
        Interpreter(
            statementInterpreters = default10StatementInterpreters(output),
            expressionEvaluators = default10ExpressionEvaluators(),
        )

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

    fun default10ExpressionEvaluators(): List<Handler<Expression, InterpreterContext, Value>> =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            IdentifierEvaluator(),
            BinaryExpressionEvaluator(),
        )

    fun default11StatementInterpreters(output: Output): List<Handler<Statement, InterpreterContext, Unit>> =
        listOf(
            VariableDeclaration11Interpreter(),
            ConstDeclarationInterpreter(),
            Assignment11Interpreter(),
            IfStatementInterpreter(),
            BlockInterpreter(),
            PrintCallInterpreter(output),
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
