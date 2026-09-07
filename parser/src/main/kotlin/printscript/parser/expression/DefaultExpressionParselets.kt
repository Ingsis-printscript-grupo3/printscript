package printscript.parser.expression

import printscript.common.TokenType

object DefaultExpressionParselets {
    val infix: Map<TokenType, InfixParselet> =
        mapOf(
            TokenType.PLUS to BinaryOperatorParselet(precedence = 1),
            TokenType.MINUS to BinaryOperatorParselet(precedence = 1),
            TokenType.MULTIPLY to BinaryOperatorParselet(precedence = 2),
            TokenType.DIVIDE to BinaryOperatorParselet(precedence = 2),
        )
}
