package printscript.parser

import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.expression.PrefixParselet
import printscript.parser.result.ASTResult
import printscript.parser.result.ParseResult
import printscript.parser.stream.TokenStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserFactoryTest {
    private fun declaration() = tokensOf("let x: number = 5;")

    private fun parse(parser: Parser) = parser.parse().asSequence().toList()

    @Test
    fun `create instantiates a parser with given parselets and handlers`() {
        val parser =
            ParserFactory.create(
                tokens = declaration(),
                prefixParselets = testPrefixParselets(LanguageVersion.V1_0),
                statementHandlers = testStatementHandlers(LanguageVersion.V1_0),
            )
        val results = parse(parser)

        assertEquals(1, results.size)
        assertIs<ParseResult.Success>(results[0])
    }

    @Test
    fun `default infix parselets are the binary operators`() {
        val infix = ParserFactory.defaultInfixParselets()

        assertEquals(
            setOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE),
            infix.keys,
        )
    }

    @Test
    fun `parselets can be overridden through the factory`() {
        val constantParselet =
            object : PrefixParselet {
                override fun parse(
                    token: Token,
                    stream: TokenStream,
                    expressionParser: ExpressionParser,
                ): ASTResult<Expression> = ASTResult.Success(NumberLiteral(777.0))
            }

        val parser =
            ParserFactory.create(
                tokens = declaration(),
                prefixParselets =
                    testPrefixParselets(LanguageVersion.V1_0) +
                        (TokenType.NUMBERLITERAL to constantParselet),
                statementHandlers = testStatementHandlers(LanguageVersion.V1_0),
            )

        val declaration = assertIs<VariableDeclaration>(assertIs<ParseResult.Success>(parse(parser)[0]).statement)
        assertEquals(777.0, assertIs<NumberLiteral>(declaration.value).value)
    }
}
