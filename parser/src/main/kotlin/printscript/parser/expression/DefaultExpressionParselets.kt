package printscript.parser.expression

import printscript.common.TokenType

object DefaultExpressionParselets {
    const val PREC_SUM = 10
    const val PREC_PRODUCT = 20
    const val PREC_UNARY = 30

    val infix: Map<TokenType, InfixParselet> =
        mapOf(
            TokenType.PLUS to BinaryOperatorParselet(precedence = PREC_SUM),
            TokenType.MINUS to BinaryOperatorParselet(precedence = PREC_SUM),
            TokenType.MULTIPLY to BinaryOperatorParselet(precedence = PREC_PRODUCT),
            TokenType.DIVIDE to BinaryOperatorParselet(precedence = PREC_PRODUCT),
        )
}
