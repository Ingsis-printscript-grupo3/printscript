package printscript.runner.version

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.interpreter.Value
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.input.InputProvider
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.InterpreterContext
import printscript.parser.expression.InfixParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.statement.StatementHandler
import printscript.semantic.SemanticExpressionHandler
import printscript.semantic.SemanticStatementHandler

// lo que trae cada version vive en :runner, porque es el modulo que integra todas las piezas: los
// modulos del nucleo no conocen versiones. Cada version lista todas sus piezas. Las del interprete
// son funciones y no listas fijas porque sus handlers reciben el output, el input y el env de cada ejecucion.
// Las demas piezas se crean una sola vez y las comparten todas las ejecuciones: no pueden guardar estado
internal interface LanguageDefinition {
    val keywords: Map<String, TokenType>
    val prefixParselets: Map<TokenType, PrefixParselet>
    val infixParselets: Map<TokenType, InfixParselet>
    val statementHandlers: Map<TokenType, StatementHandler>
    val semanticStatements: List<SemanticStatementHandler>
    val semanticExpressions: List<SemanticExpressionHandler>

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
