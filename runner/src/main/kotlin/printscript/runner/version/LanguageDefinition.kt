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

// las piezas del interprete son funciones porque necesitan el output, el input y el env de cada ejecucion.
// Las demas se crean una vez y se comparten entre ejecuciones, asi que no pueden guardar estado
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
