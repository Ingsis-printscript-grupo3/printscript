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
import printscript.parser.expression.BooleanLiteralParselet
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.expression.IdentifierParselet
import printscript.parser.expression.InfixParselet
import printscript.parser.expression.NumberLiteralParselet
import printscript.parser.expression.ParenthesizedExpressionParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.expression.ReadEnvParselet
import printscript.parser.expression.ReadInputParselet
import printscript.parser.expression.StringLiteralParselet
import printscript.parser.expression.UnaryMinusParselet
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.handlers.AssignmentHandler
import printscript.parser.statement.handlers.IfStatementHandler
import printscript.parser.statement.handlers.PrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler
import printscript.semantic.SemanticExpressionHandler
import printscript.semantic.SemanticStatementHandler
import printscript.semantic.handler.expression.BinaryExpressionHandler
import printscript.semantic.handler.expression.BooleanLiteralHandler
import printscript.semantic.handler.expression.IdentifierHandler
import printscript.semantic.handler.expression.NumberLiteralHandler
import printscript.semantic.handler.expression.ReadEnvHandler
import printscript.semantic.handler.expression.ReadInputHandler
import printscript.semantic.handler.expression.StringLiteralHandler
import printscript.semantic.handler.statement.AssignmentHandler as SemanticAssignmentHandler
import printscript.semantic.handler.statement.BlockHandler as SemanticBlockHandler
import printscript.semantic.handler.statement.IfStatementHandler as SemanticIfStatementHandler
import printscript.semantic.handler.statement.PrintCallHandler as SemanticPrintCallHandler
import printscript.semantic.handler.statement.VariableDeclarationHandler as SemanticVariableDeclarationHandler

internal object PrintScript11 : LanguageDefinition {
    override val keywords: Map<String, TokenType> =
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

    override val prefixParselets: Map<TokenType, PrefixParselet> =
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

    override val infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix

    private val declarableTypes = setOf(TokenType.NUMBERTYPE, TokenType.STRINGTYPE, TokenType.BOOLEANTYPE)

    override val statementHandlers: Map<TokenType, StatementHandler> =
        mapOf(
            TokenType.LET to VariableDeclarationHandler(declarableTypes),
            TokenType.PRINTLN to PrintCallHandler,
            TokenType.IDENTIFIER to AssignmentHandler,
            TokenType.CONST to VariableDeclarationHandler(declarableTypes),
            TokenType.IF to IfStatementHandler,
        )

    override val semanticStatements: List<SemanticStatementHandler> =
        listOf(
            SemanticVariableDeclarationHandler(),
            SemanticAssignmentHandler(),
            SemanticPrintCallHandler(),
            SemanticIfStatementHandler(),
            SemanticBlockHandler(),
        )

    override val semanticExpressions: List<SemanticExpressionHandler> =
        listOf(
            NumberLiteralHandler(),
            StringLiteralHandler(),
            BooleanLiteralHandler(),
            IdentifierHandler(),
            BinaryExpressionHandler(),
            ReadInputHandler(),
            ReadEnvHandler(),
        )

    override fun statementInterpreters(output: Output): List<Handler<Statement, InterpreterContext, Unit>> =
        listOf(
            VariableDeclarationInterpreter(),
            AssignmentInterpreter(),
            PrintCallInterpreter(output),
            IfStatementInterpreter(),
            BlockInterpreter(),
        )

    override fun expressionEvaluators(
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
