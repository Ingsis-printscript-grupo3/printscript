package printscript.parser

import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.expression.PrefixParselet
import printscript.parser.result.ASTResult
import printscript.parser.result.ParseResult
import printscript.parser.stream.TokenStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParserFactoryTest {
    private fun token(
        type: TokenType,
        value: String = "",
    ) = Token(type, Position(1, 1), Position(1, 1), value)

    // let x: number = 5;
    private fun declaration() =
        listOf(
            token(TokenType.LET, "let"),
            token(TokenType.IDENTIFIER, "x"),
            token(TokenType.COLON, ":"),
            token(TokenType.NUMBERTYPE, "number"),
            token(TokenType.ASSIGN, "="),
            token(TokenType.NUMBERLITERAL, "5"),
            token(TokenType.SEMICOLON, ";"),
            token(TokenType.EOF),
        ).iterator()

    // const c: boolean = true;  -- solo existe en 1.1
    private fun constDeclaration() =
        listOf(
            token(TokenType.CONST, "const"),
            token(TokenType.IDENTIFIER, "c"),
            token(TokenType.COLON, ":"),
            token(TokenType.BOOLEANTYPE, "boolean"),
            token(TokenType.ASSIGN, "="),
            token(TokenType.BOOLEANLITERAL, "true"),
            token(TokenType.SEMICOLON, ";"),
            token(TokenType.EOF),
        ).iterator()

    private fun parse(parser: ParserInterface) = parser.parse().asSequence().toList()

    @Test
    fun `create10 parses what 1_0 supports`() {
        val results = parse(ParserFactory.create10(declaration()))

        assertEquals(1, results.size)
        assertIs<ParseResult.Success>(results[0])
    }

    @Test
    fun `create10 rejects a 1_1 feature naming the version that has it`() {
        val results = parse(ParserFactory.create10(constDeclaration()))

        val failure = assertIs<ParseResult.Failure>(results[0])
        assertTrue(failure.message.contains("1.1"), "expected the failure to name 1.1, was: ${failure.message}")
    }

    @Test
    fun `create11 parses const declarations`() {
        val results = parse(ParserFactory.create11(constDeclaration()))

        assertEquals(1, results.size)
        assertIs<ParseResult.Success>(results[0])
    }

    @Test
    fun `create with a version string matches create with the enum`() {
        assertIs<ParseResult.Success>(parse(ParserFactory.create(declaration(), "1.0"))[0])
        assertIs<ParseResult.Success>(parse(ParserFactory.create(constDeclaration(), "1.1"))[0])
        assertIs<ParseResult.Success>(parse(ParserFactory.create(declaration(), LanguageVersion.V1_0))[0])
    }

    @Test
    fun `create throws on an unknown version string`() {
        assertFailsWith<IllegalArgumentException> {
            ParserFactory.create(declaration(), "2.0")
        }
    }

    @Test
    fun `default statement handlers grow from 1_0 to 1_1`() {
        val handlers10 = ParserFactory.defaultStatementHandlers(LanguageVersion.V1_0)
        val handlers11 = ParserFactory.defaultStatementHandlers(LanguageVersion.V1_1)

        assertTrue(handlers10.containsKey(TokenType.LET))
        assertTrue(handlers10.containsKey(TokenType.PRINTLN))
        assertFalse(handlers10.containsKey(TokenType.CONST))
        assertFalse(handlers10.containsKey(TokenType.IF))

        assertTrue(handlers11.keys.containsAll(handlers10.keys))
        assertTrue(handlers11.containsKey(TokenType.CONST))
        assertTrue(handlers11.containsKey(TokenType.IF))
    }

    @Test
    fun `default prefix parselets grow from 1_0 to 1_1`() {
        val prefix10 = ParserFactory.defaultPrefixParselets(LanguageVersion.V1_0)
        val prefix11 = ParserFactory.defaultPrefixParselets(LanguageVersion.V1_1)

        assertTrue(prefix10.containsKey(TokenType.NUMBERLITERAL))
        assertFalse(prefix10.containsKey(TokenType.BOOLEANLITERAL))
        assertFalse(prefix10.containsKey(TokenType.READINPUT))

        assertTrue(prefix11.keys.containsAll(prefix10.keys))
        assertTrue(prefix11.containsKey(TokenType.BOOLEANLITERAL))
        assertTrue(prefix11.containsKey(TokenType.READINPUT))
        assertTrue(prefix11.containsKey(TokenType.READENV))
    }

    @Test
    fun `default infix parselets are the binary operators and do not depend on the version`() {
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
                version = LanguageVersion.V1_0,
                prefixParselets =
                    ParserFactory.defaultPrefixParselets(LanguageVersion.V1_0) +
                        (TokenType.NUMBERLITERAL to constantParselet),
            )

        val declaration = assertIs<VariableDeclaration>(assertIs<ParseResult.Success>(parse(parser)[0]).statement)
        assertEquals(777.0, assertIs<NumberLiteral>(declaration.value).value)
    }
}
