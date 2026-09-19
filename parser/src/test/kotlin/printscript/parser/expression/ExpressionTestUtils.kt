package printscript.parser.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

fun pos(
    line: Int = 1,
    column: Int = 1,
): Position = Position(line, column)

fun token(
    type: TokenType,
    value: String = "",
    line: Int = 1,
    column: Int = 1,
): Token = Token(type, pos(line, column), Position(line, column + value.length), value)

fun num(
    v: String,
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.NUMBERLITERAL, v, line, column)

fun id(
    name: String,
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.IDENTIFIER, name, line, column)

fun str(
    v: String,
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.STRINGLITERAL, v, line, column)

fun bool(
    v: String,
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.BOOLEANLITERAL, v, line, column)

fun minusToken(
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.MINUS, "-", line, column)

fun plusToken(
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.PLUS, "+", line, column)

fun multToken(
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.MULTIPLY, "*", line, column)

fun divToken(
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.DIVIDE, "/", line, column)

fun lparenToken(
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.LEFTPAREN, "(", line, column)

fun rparenToken(
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.RIGHTPAREN, ")", line, column)

fun assignToken(
    line: Int = 1,
    column: Int = 1,
): Token = token(TokenType.ASSIGN, "=", line, column)

fun parseExpression(
    vararg tokens: Token,
    version: LanguageVersion = LanguageVersion.V1_1,
): ASTResult<Expression> {
    val tokenList =
        if (tokens.isNotEmpty() && tokens.last().type == TokenType.EOF) {
            tokens.toList()
        } else {
            tokens.toList() + token(TokenType.EOF)
        }
    val stream = TokenStream(tokenList.iterator())
    val expressionParser = ExpressionParser(stream, version)
    return expressionParser.parseExpression()
}

fun parseExpressionSuccess(vararg tokens: Token): Expression {
    val result = parseExpression(*tokens)
    val success = assertIs<ASTResult.Success<Expression>>(result)
    return success.value
}

fun parseExpressionWithInfix(
    infixParselets: Map<TokenType, InfixParselet>,
    vararg tokens: Token,
    version: LanguageVersion = LanguageVersion.V1_1,
): ASTResult<Expression> {
    val tokenList =
        if (tokens.isNotEmpty() && tokens.last().type == TokenType.EOF) {
            tokens.toList()
        } else {
            tokens.toList() + token(TokenType.EOF)
        }
    val stream = TokenStream(tokenList.iterator())
    val expressionParser = ExpressionParser(stream, version, infixParselets = infixParselets)
    return expressionParser.parseExpression()
}

fun parseExpressionSuccessWithInfix(
    infixParselets: Map<TokenType, InfixParselet>,
    vararg tokens: Token,
): Expression {
    val result = parseExpressionWithInfix(infixParselets, *tokens)
    val success = assertIs<ASTResult.Success<Expression>>(result)
    return success.value
}

fun assertDesugaredMinus(expr: Expression): Expression {
    val binary = assertIs<BinaryExpression>(expr)
    assertEquals(TokenType.MINUS, binary.operator)
    val left = assertIs<NumberLiteral>(binary.left)
    assertEquals(0.0, left.value)
    return binary.right
}

fun assertNumber(
    expr: Expression,
    expectedValue: Double,
): NumberLiteral {
    val literal = assertIs<NumberLiteral>(expr)
    assertEquals(expectedValue, literal.value)
    return literal
}

fun assertId(
    expr: Expression,
    expectedName: String,
): Identifier {
    val identifier = assertIs<Identifier>(expr)
    assertEquals(expectedName, identifier.name)
    return identifier
}

fun assertBinary(
    expr: Expression,
    expectedOperator: TokenType,
): BinaryExpression {
    val binary = assertIs<BinaryExpression>(expr)
    assertEquals(expectedOperator, binary.operator)
    return binary
}

fun assertParseFailure(
    vararg tokens: Token,
    expectedMessageSubstring: String = "Expected a value or expression",
): ASTResult.Failure {
    val result = parseExpression(*tokens)
    val failure = assertIs<ASTResult.Failure>(result)
    assertTrue(
        failure.message.contains(expectedMessageSubstring),
        "Expected failure message containing '$expectedMessageSubstring' but was '${failure.message}'",
    )
    return failure
}
