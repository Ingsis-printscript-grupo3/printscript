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

    fun prefix(version: LanguageVersion): Map<TokenType, PrefixParselet> =
        when (version) {
            LanguageVersion.V1_0 -> prefix10
            LanguageVersion.V1_1 -> prefix10 + prefix11
        }

    // lo que ya parseaba 1.0
    private val prefix10: Map<TokenType, PrefixParselet> =
        mapOf(
            TokenType.NUMBERLITERAL to NumberLiteralParselet,
            TokenType.STRINGLITERAL to StringLiteralParselet,
            TokenType.IDENTIFIER to IdentifierParselet,
            TokenType.LEFTPAREN to ParenthesizedExpressionParselet,
        )

    // 1.1 se monta sobre 1.0. En 1.0 estas claves no estan, asi que el ExpressionParser
    // no encuentra parselet y VersionFeatures da el error
    private val prefix11: Map<TokenType, PrefixParselet> =
        mapOf(
            TokenType.BOOLEANLITERAL to BooleanLiteralParselet,
            TokenType.READINPUT to ReadInputParselet,
            TokenType.READENV to ReadEnvParselet,
        )
}
