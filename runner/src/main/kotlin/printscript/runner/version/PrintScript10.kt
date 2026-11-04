package printscript.runner.version

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.common.TokenType
import printscript.interpreter.Value
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.input.InputProvider
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.expression.IdentifierParselet
import printscript.parser.expression.InfixParselet
import printscript.parser.expression.NumberLiteralParselet
import printscript.parser.expression.ParenthesizedExpressionParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.expression.StringLiteralParselet
import printscript.parser.expression.UnaryMinusParselet
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.handlers.AssignmentHandler
import printscript.parser.statement.handlers.PrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler
import printscript.semantic.SemanticExpressionHandler
import printscript.semantic.SemanticStatementHandler
import printscript.semantic.handler.expression.BinaryExpressionHandler
import printscript.semantic.handler.expression.IdentifierHandler
import printscript.semantic.handler.expression.NumberLiteralHandler
import printscript.semantic.handler.expression.StringLiteralHandler
import printscript.semantic.handler.statement.AssignmentHandler as SemanticAssignmentHandler
import printscript.semantic.handler.statement.PrintCallHandler as SemanticPrintCallHandler
import printscript.semantic.handler.statement.VariableDeclarationHandler as SemanticVariableDeclarationHandler

internal object PrintScript10 : LanguageDefinition {
    override val keywords: Map<String, TokenType> =
        mapOf(
            "let" to TokenType.LET,
            "println" to TokenType.PRINTLN,
            "number" to TokenType.NUMBERTYPE,
            "string" to TokenType.STRINGTYPE,
        )

    override val prefixParselets: Map<TokenType, PrefixParselet> =
        mapOf(
            TokenType.NUMBERLITERAL to NumberLiteralParselet,
            TokenType.STRINGLITERAL to StringLiteralParselet,
            TokenType.IDENTIFIER to IdentifierParselet,
            TokenType.LEFTPAREN to ParenthesizedExpressionParselet,
            TokenType.MINUS to UnaryMinusParselet(),
        )

    override val infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix

    private val declarableTypes = setOf(TokenType.NUMBERTYPE, TokenType.STRINGTYPE)

    override val statementHandlers: Map<TokenType, StatementHandler> =
        mapOf(
            TokenType.LET to VariableDeclarationHandler(declarableTypes),
            TokenType.PRINTLN to PrintCallHandler,
            TokenType.IDENTIFIER to AssignmentHandler,
        )

    override val semanticStatements: List<SemanticStatementHandler> =
        listOf(
            SemanticVariableDeclarationHandler(),
            SemanticAssignmentHandler(),
            SemanticPrintCallHandler(),
        )

    override val semanticExpressions: List<SemanticExpressionHandler> =
        listOf(
            NumberLiteralHandler(),
            StringLiteralHandler(),
            IdentifierHandler(),
            BinaryExpressionHandler(),
        )

    override fun statementInterpreters(output: Output): List<Handler<Statement, InterpreterContext, Unit>> =
        listOf(
            VariableDeclarationInterpreter(),
            AssignmentInterpreter(),
            PrintCallInterpreter(output),
        )

    // input y env se descartan a proposito: 1.0 no tiene readInput ni readEnv
    override fun expressionEvaluators(
        input: InputProvider,
        env: EnvProvider,
    ): List<Handler<Expression, InterpreterContext, Value>> =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            IdentifierEvaluator(),
            BinaryExpressionEvaluator(),
        )
}
