package printscript.parser.expression

import printscript.common.LanguageVersion
import printscript.common.TokenType

object DefaultExpressionParselets {
    val infix: Map<TokenType, InfixParselet> =
        mapOf(
            TokenType.PLUS to BinaryOperatorParselet(precedence = 1),
            TokenType.MINUS to BinaryOperatorParselet(precedence = 1),
            TokenType.MULTIPLY to BinaryOperatorParselet(precedence = 2),
            TokenType.DIVIDE to BinaryOperatorParselet(precedence = 2),
        )

    fun prefix(version: LanguageVersion = LanguageVersion.V1_1): Map<TokenType, PrefixParselet> =
        mapOf(
            TokenType.NUMBERLITERAL to NumberLiteralParselet,
            TokenType.STRINGLITERAL to StringLiteralParselet,
            TokenType.IDENTIFIER to IdentifierParselet,
            TokenType.LEFTPAREN to ParenthesizedExpressionParselet,
            TokenType.BOOLEANLITERAL to BooleanLiteralParselet(version),
            TokenType.READINPUT to ReadInputParselet(version),
            TokenType.READENV to ReadEnvParselet(version),
        )
}
