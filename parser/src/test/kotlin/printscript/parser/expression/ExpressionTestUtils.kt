package printscript.parser.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import printscript.parser.testPrefixParselets
import printscript.parser.tokensOf
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal fun parseExpression(
    code: String,
    version: LanguageVersion = LanguageVersion.V1_1,
    infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix,
): ASTResult<Expression> =
    ExpressionParser(
        TokenStream(tokensOf(code, version)),
        testPrefixParselets(version),
        infixParselets = infixParselets,
    ).parseExpression()

internal fun parseExpressionSuccess(code: String): Expression =
    assertIs<ASTResult.Success<Expression>>(parseExpression(code)).value

internal fun parseExpressionSuccessWithInfix(
    infixParselets: Map<TokenType, InfixParselet>,
    code: String,
): Expression = assertIs<ASTResult.Success<Expression>>(parseExpression(code, infixParselets = infixParselets)).value

internal fun assertDesugaredMinus(expr: Expression): Expression {
    val binary = assertIs<BinaryExpression>(expr)
    assertEquals(TokenType.MINUS, binary.operator)
    val left = assertIs<NumberLiteral>(binary.left)
    assertEquals(0.0, left.value)
    return binary.right
}

internal fun assertNumber(
    expr: Expression,
    expectedValue: Double,
): NumberLiteral {
    val literal = assertIs<NumberLiteral>(expr)
    assertEquals(expectedValue, literal.value)
    return literal
}

internal fun assertId(
    expr: Expression,
    expectedName: String,
): Identifier {
    val identifier = assertIs<Identifier>(expr)
    assertEquals(expectedName, identifier.name)
    return identifier
}

internal fun assertBinary(
    expr: Expression,
    expectedOperator: TokenType,
): BinaryExpression {
    val binary = assertIs<BinaryExpression>(expr)
    assertEquals(expectedOperator, binary.operator)
    return binary
}

internal fun assertParseFailure(
    code: String,
    expectedMessageSubstring: String = "Expected a value or expression",
): ASTResult.Failure {
    val result = parseExpression(code)
    val failure = assertIs<ASTResult.Failure>(result)
    assertTrue(
        failure.message.contains(expectedMessageSubstring),
        "Expected failure message containing '$expectedMessageSubstring' but was '${failure.message}'",
    )
    return failure
}
