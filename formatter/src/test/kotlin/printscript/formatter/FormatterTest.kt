package printscript.formatter
import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.TokenType
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class FormatterTest {
    private fun format(
        statements: List<Statement>,
        rules: FormatterRules = FormatterRules(),
    ): String {
        val writer = StringWriter()
        Formatter(rules).format(statements.iterator(), writer)
        return writer.toString()
    }

    @Test
    fun `formats a variable declaration with an initial value using default rules`() {
        val statements = listOf(VariableDeclaration("x", "number", NumberLiteral(5.0)))

        assertEquals("let x:number = 5;\n", format(statements))
    }

    @Test
    fun `formats a variable declaration without an initial value`() {
        val statements = listOf(VariableDeclaration("x", "number", null))

        assertEquals("let x:number;\n", format(statements))
    }

    @Test
    fun `formats an assignment`() {
        val statements = listOf(Assignment("x", NumberLiteral(10.0)))

        assertEquals("x = 10;\n", format(statements))
    }

    @Test
    fun `formats a println call with a string literal`() {
        val statements = listOf(PrintCall(StringLiteral("hello")))

        assertEquals("println(\"hello\");\n", format(statements))
    }

    @Test
    fun `formats an identifier`() {
        val statements = listOf(Assignment("y", Identifier("x")))

        assertEquals("y = x;\n", format(statements))
    }

    @Test
    fun `keeps the decimals of a number that has them`() {
        val statements = listOf(Assignment("x", NumberLiteral(3.5)))

        assertEquals("x = 3.5;\n", format(statements))
    }

    @Test
    fun `writes a negative whole number without decimals`() {
        val statements = listOf(Assignment("x", NumberLiteral(-7.0)))

        assertEquals("x = -7;\n", format(statements))
    }

    @Test
    fun `formats a binary expression with one space before and after the operator`() {
        val expr = BinaryExpression(Identifier("x"), TokenType.PLUS, NumberLiteral(3.0))
        val statements = listOf(Assignment("y", expr))

        assertEquals("y = x + 3;\n", format(statements))
    }

    @Test
    fun `formats every supported binary operator`() {
        val operators =
            mapOf(
                TokenType.PLUS to "+",
                TokenType.MINUS to "-",
                TokenType.MULTIPLY to "*",
                TokenType.DIVIDE to "/",
            )

        operators.forEach { (tokenType, symbol) ->
            val expr = BinaryExpression(NumberLiteral(1.0), tokenType, NumberLiteral(2.0))
            val statements = listOf(Assignment("y", expr))

            assertEquals("y = 1 $symbol 2;\n", format(statements))
        }
    }

    @Test
    fun `fails when the operator is not one of the supported ones`() {
        val expr = BinaryExpression(NumberLiteral(1.0), TokenType.ASSIGN, NumberLiteral(2.0))
        val statements = listOf(Assignment("y", expr))

        assertFailsWith<IllegalStateException> { format(statements) }
    }

    @Test
    fun `wraps a sum in parentheses when it is multiplied`() {
        val sum = BinaryExpression(NumberLiteral(2.0), TokenType.PLUS, NumberLiteral(3.0))
        val expr = BinaryExpression(sum, TokenType.MULTIPLY, NumberLiteral(4.0))
        val statements = listOf(Assignment("y", expr))

        assertEquals("y = (2 + 3) * 4;\n", format(statements))
    }

    @Test
    fun `does not add parentheses when the product already binds tighter`() {
        val product = BinaryExpression(NumberLiteral(3.0), TokenType.MULTIPLY, NumberLiteral(4.0))
        val expr = BinaryExpression(NumberLiteral(2.0), TokenType.PLUS, product)
        val statements = listOf(Assignment("y", expr))

        assertEquals("y = 2 + 3 * 4;\n", format(statements))
    }

    @Test
    fun `wraps a subtraction that sits on the right of another subtraction`() {
        val inner = BinaryExpression(Identifier("b"), TokenType.MINUS, Identifier("c"))
        val expr = BinaryExpression(Identifier("a"), TokenType.MINUS, inner)
        val statements = listOf(Assignment("y", expr))

        assertEquals("y = a - (b - c);\n", format(statements))
    }

    @Test
    fun `does not wrap a subtraction that sits on the left of another subtraction`() {
        val inner = BinaryExpression(Identifier("a"), TokenType.MINUS, Identifier("b"))
        val expr = BinaryExpression(inner, TokenType.MINUS, Identifier("c"))
        val statements = listOf(Assignment("y", expr))

        assertEquals("y = a - b - c;\n", format(statements))
    }

    @Test
    fun `wraps a division that sits on the right of another division`() {
        val inner = BinaryExpression(Identifier("b"), TokenType.DIVIDE, Identifier("c"))
        val expr = BinaryExpression(Identifier("a"), TokenType.DIVIDE, inner)
        val statements = listOf(Assignment("y", expr))

        assertEquals("y = a / (b / c);\n", format(statements))
    }

    @Test
    fun `does not wrap a sum that sits on the right of another sum`() {
        val inner = BinaryExpression(Identifier("b"), TokenType.PLUS, Identifier("c"))
        val expr = BinaryExpression(Identifier("a"), TokenType.PLUS, inner)
        val statements = listOf(Assignment("y", expr))

        assertEquals("y = a + b + c;\n", format(statements))
    }

    @Test
    fun `adds a space before the colon when the rule is on`() {
        val statements = listOf(VariableDeclaration("x", "number", null))
        val rules = FormatterRules(spaceBeforeColon = true)

        assertEquals("let x :number;\n", format(statements, rules))
    }

    @Test
    fun `adds a space after the colon when the rule is on`() {
        val statements = listOf(VariableDeclaration("x", "number", null))
        val rules = FormatterRules(spaceAfterColon = true)

        assertEquals("let x: number;\n", format(statements, rules))
    }

    @Test
    fun `does not add spaces around the equals sign in a declaration when the rule is off`() {
        val statements = listOf(VariableDeclaration("x", "number", NumberLiteral(5.0)))
        val rules = FormatterRules(noSpacingAroundEquals = true)

        assertEquals("let x:number=5;\n", format(statements, rules))
    }

    @Test
    fun `does not add spaces around the equals sign in an assignment when the rule is off`() {
        val statements = listOf(Assignment("x", NumberLiteral(5.0)))
        val rules = FormatterRules(noSpacingAroundEquals = true)

        assertEquals("x=5;\n", format(statements, rules))
    }

    @Test
    fun `every statement goes on its own line, with no extra breaks by default`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                PrintCall(Identifier("x")),
            )

        assertEquals("let x:number = 1;\nprintln(x);\n", format(statements))
    }

    @Test
    fun `asking for 0 extra breaks still leaves the mandatory one`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                PrintCall(Identifier("x")),
            )
        val rules = FormatterRules(lineBreaksAfterPrintln = 0)

        assertEquals("let x:number = 1;\nprintln(x);\n", format(statements, rules))
    }

    @Test
    fun `puts one blank line after the println when the rule is 1`() {
        val statements = listOf(PrintCall(NumberLiteral(1.0)), PrintCall(NumberLiteral(2.0)))
        val rules = FormatterRules(lineBreaksAfterPrintln = 1)

        assertEquals("println(1);\n\nprintln(2);\n", format(statements, rules))
    }

    @Test
    fun `puts two blank lines after the println when the rule is 2`() {
        val statements = listOf(PrintCall(NumberLiteral(1.0)), PrintCall(NumberLiteral(2.0)))
        val rules = FormatterRules(lineBreaksAfterPrintln = 2)

        assertEquals("println(1);\n\n\nprintln(2);\n", format(statements, rules))
    }

    @Test
    fun `the breaks go after the println, not before it`() {
        val statements = listOf(PrintCall(NumberLiteral(1.0)), VariableDeclaration("x", "number", null))
        val rules = FormatterRules(lineBreaksAfterPrintln = 2)

        assertEquals("println(1);\n\n\nlet x:number;\n", format(statements, rules))
    }

    @Test
    fun `no extra breaks are added after the last statement`() {
        val statements = listOf(PrintCall(NumberLiteral(1.0)))
        val rules = FormatterRules(lineBreaksAfterPrintln = 2)

        assertEquals("println(1);\n", format(statements, rules))
    }

    @Test
    fun `formats an empty statement list as an empty string`() {
        assertEquals("\n", format(emptyList()))
    }

    @Test
    fun `formats several statements together end to end`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(5.0)),
                Assignment("x", BinaryExpression(Identifier("x"), TokenType.PLUS, NumberLiteral(1.0))),
                PrintCall(Identifier("x")),
                VariableDeclaration("greeting", "string", StringLiteral("hello")),
                PrintCall(Identifier("greeting")),
            )

        val expected =
            "let x:number = 5;\n" +
                "x = x + 1;\n" +
                "println(x);\n" +
                "let greeting:string = \"hello\";\n" +
                "println(greeting);\n"

        assertEquals(expected, format(statements))
    }

    @Test
    fun `the rules the config cannot turn off are always applied`() {
        val statements =
            listOf(
                VariableDeclaration(
                    "x",
                    "number",
                    BinaryExpression(NumberLiteral(1.0), TokenType.PLUS, NumberLiteral(2.0)),
                ),
                PrintCall(Identifier("x")),
            )
        val rules =
            FormatterRules(
                spaceBeforeColon = true,
                spaceAfterColon = true,
                noSpacingAroundEquals = true,
                lineBreaksAfterPrintln = 2,
            )

        val output = format(statements, rules)

        assertEquals("let x : number=1 + 2;\nprintln(x);\n", output)
        assertFalse(output.contains("  "))
    }
}
