package printscript.runner.version

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.common.LanguageVersion
import printscript.interpreter.Value
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.input.InputProvider
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.InterpreterContext

// cada version lista todas sus piezas. Las del interprete son funciones y no listas fijas porque
// sus handlers reciben el output, el input y el env de cada ejecucion
internal interface LanguageDefinition {
    fun statementInterpreters(output: Output): List<Handler<Statement, InterpreterContext, Unit>>

    fun expressionEvaluators(
        input: InputProvider,
        env: EnvProvider,
    ): List<Handler<Expression, InterpreterContext, Value>>
}

internal fun LanguageVersion.definition(): LanguageDefinition =
    when (this) {
        LanguageVersion.V1_0 -> PrintScript10
        LanguageVersion.V1_1 -> PrintScript11
    }
