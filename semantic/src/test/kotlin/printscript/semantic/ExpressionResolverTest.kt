package printscript.semantic

import printscript.ast.BinaryExpression
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.StringLiteral
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpressionResolverTest {
    private fun resolver(
        symbolTable: SymbolTable = SymbolTable(),
        version: LanguageVersion = LanguageVersion.V1_1,
    ) = ExpressionResolver(symbolTable, version)

    @Test
    fun `resolves number literals`() {
        val result = resolver().resolveType(NumberLiteral(1.0))

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `resolves string literals`() {
        val result = resolver().resolveType(StringLiteral("hi"))

        assertEquals(SemanticResult.Success("string"), result)
    }

    @Test
    fun `resolves a declared identifier to its declared type`() {
        val symbolTable = SymbolTable()
        symbolTable.define("a", "number")

        val result = resolver(symbolTable).resolveType(Identifier("a"))

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `fails resolving an undeclared identifier`() {
        val result = resolver().resolveType(Identifier("missing"))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `adding two numbers yields number`() {
        val result = resolver().resolveType(BinaryExpression(NumberLiteral(1.0), TokenType.PLUS, NumberLiteral(2.0)))

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `adding a number and a string yields string`() {
        val result = resolver().resolveType(BinaryExpression(NumberLiteral(1.0), TokenType.PLUS, StringLiteral("x")))

        assertEquals(SemanticResult.Success("string"), result)
    }

    @Test
    fun `subtracting two numbers yields number`() {
        val result = resolver().resolveType(BinaryExpression(NumberLiteral(5.0), TokenType.MINUS, NumberLiteral(2.0)))

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `subtracting a string is a type error`() {
        val result = resolver().resolveType(BinaryExpression(StringLiteral("x"), TokenType.MINUS, NumberLiteral(2.0)))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `a failure on the left operand short-circuits the binary expression`() {
        val result =
            resolver().resolveType(
                BinaryExpression(Identifier("missing"), TokenType.PLUS, NumberLiteral(2.0)),
            )

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `an expression with no handler registered fails explicitly with the node position`() {
        val emptyRegistry = Registry<Expression, ExpressionResolver, SemanticResult<String>>()
        val resolver = ExpressionResolver(SymbolTable(), LanguageVersion.V1_1, emptyRegistry)
        val node = NumberLiteral(1.0, printscript.common.Position(4, 2))

        val result = resolver.resolveType(node)

        assertIs<SemanticResult.Failure>(result)
        assertEquals(node.position, result.position)
    }

    @Test
    fun `resolves boolean literals`() {
        val trueResult = resolver().resolveType(BooleanLiteral(true))
        assertEquals(SemanticResult.Success("boolean"), trueResult)

        val falseResult = resolver().resolveType(BooleanLiteral(false))
        assertEquals(SemanticResult.Success("boolean"), falseResult)
    }

    @Test
    fun `adding boolean and number is rejected`() {
        val expr1 = BinaryExpression(BooleanLiteral(true), TokenType.PLUS, NumberLiteral(1.0))
        val result1 = resolver().resolveType(expr1)
        assertIs<SemanticResult.Failure>(result1)

        val expr2 = BinaryExpression(NumberLiteral(1.0), TokenType.PLUS, BooleanLiteral(false))
        val result2 = resolver().resolveType(expr2)
        assertIs<SemanticResult.Failure>(result2)
    }

    @Test
    fun `adding boolean and string is rejected`() {
        val expr1 = BinaryExpression(StringLiteral("hello"), TokenType.PLUS, BooleanLiteral(true))
        val result1 = resolver().resolveType(expr1)
        assertIs<SemanticResult.Failure>(result1)

        val expr2 = BinaryExpression(BooleanLiteral(true), TokenType.PLUS, StringLiteral("hello"))
        val result2 = resolver().resolveType(expr2)
        assertIs<SemanticResult.Failure>(result2)
    }

    @Test
    fun `arithmetic operations between booleans are rejected`() {
        val ops = listOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE)
        for (op in ops) {
            val expr = BinaryExpression(BooleanLiteral(true), op, BooleanLiteral(false))
            val result = resolver().resolveType(expr)
            assertIs<SemanticResult.Failure>(result)
        }
    }

    @Test
    fun `resolves readInput with default fallback string type when expectedType is null`() {
        val result = resolver().resolveType(ReadInput(StringLiteral("Name:")))
        assertEquals(SemanticResult.Success("string"), result)
    }

    @Test
    fun `resolves readInput to expectedType when specified`() {
        val r = resolver()
        assertEquals(SemanticResult.Success("number"), r.resolveType(ReadInput(StringLiteral("Age:")), "number"))
        assertEquals(SemanticResult.Success("boolean"), r.resolveType(ReadInput(StringLiteral("Active?:")), "boolean"))
        assertEquals(SemanticResult.Success("string"), r.resolveType(ReadInput(StringLiteral("Name:")), "string"))
    }

    @Test
    fun `resolves readEnv with default fallback string type when expectedType is null`() {
        val result = resolver().resolveType(ReadEnv(StringLiteral("ENV_VAR")))
        assertEquals(SemanticResult.Success("string"), result)
    }

    @Test
    fun `resolves readEnv to expectedType when specified`() {
        val r = resolver()
        assertEquals(SemanticResult.Success("number"), r.resolveType(ReadEnv(StringLiteral("PORT")), "number"))
        assertEquals(SemanticResult.Success("boolean"), r.resolveType(ReadEnv(StringLiteral("DEBUG")), "boolean"))
        assertEquals(SemanticResult.Success("string"), r.resolveType(ReadEnv(StringLiteral("HOST")), "string"))
    }

    @Test
    fun `fails resolving readInput when argument is not string`() {
        val result = resolver().resolveType(ReadInput(NumberLiteral(42.0)))
        assertIs<SemanticResult.Failure>(result)
        assertEquals("'readInput' argument must be a string, found 'number'.", result.message)
    }

    @Test
    fun `fails resolving readInput when argument is boolean literal`() {
        val result = resolver().resolveType(ReadInput(BooleanLiteral(true)))
        assertIs<SemanticResult.Failure>(result)
        assertEquals("'readInput' argument must be a string, found 'boolean'.", result.message)
    }

    @Test
    fun `fails resolving readInput when argument is undeclared identifier`() {
        val result = resolver().resolveType(ReadInput(Identifier("unknownPrompt")))
        assertIs<SemanticResult.Failure>(result)
        assertEquals("Variable 'unknownPrompt' not declared.", result.message)
    }

    @Test
    fun `fails resolving readEnv when argument is not string`() {
        val result = resolver().resolveType(ReadEnv(NumberLiteral(10.0)))
        assertIs<SemanticResult.Failure>(result)
        assertEquals("'readEnv' argument must be a string, found 'number'.", result.message)
    }

    @Test
    fun `fails resolving readEnv when argument is undeclared identifier`() {
        val result = resolver().resolveType(ReadEnv(Identifier("unknownEnv")))
        assertIs<SemanticResult.Failure>(result)
        assertEquals("Variable 'unknownEnv' not declared.", result.message)
    }

    @Test
    fun `resolves readInput when argument is a binary string concatenation`() {
        val arg = BinaryExpression(StringLiteral("Hello "), TokenType.PLUS, StringLiteral("World: "))
        val result = resolver().resolveType(ReadInput(arg))
        assertEquals(SemanticResult.Success("string"), result)
    }

    @Test
    fun `resolves readInput when argument is a string variable from symbol table`() {
        val table = SymbolTable()
        table.define("prompt", "string")
        val result = resolver(table).resolveType(ReadInput(Identifier("prompt")))
        assertEquals(SemanticResult.Success("string"), result)
    }

    @Test
    fun `resolves nested readInput calls where inner readInput serves as prompt`() {
        val inner = ReadInput(StringLiteral("Enter prompt: "))
        val outer = ReadInput(inner)
        val result = resolver().resolveType(outer, "number")
        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `fails resolving readInput when expectedType is not supported`() {
        val result = resolver().resolveType(ReadInput(StringLiteral("Prompt:")), "unknownType")
        assertIs<SemanticResult.Failure>(result)
        assertEquals("Type 'unknownType' is not supported in PrintScript 1.1.", result.message)
    }
}
