import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrintScriptFormatterTest {
    private fun format(
        statements: List<Statement>,
        rules: FormatterRules = FormatterRules(),
    ): String = PrintScriptFormatter(rules).format(statements)

    @Test
    fun `formats a variable declaration with an initial value using default rules`() {
        val statements = listOf(VariableDeclaration("x", "number", NumberLiteral(5.0)))

        assertEquals("let x: number = 5.0;\n", format(statements))
    }

    @Test
    fun `formats a variable declaration without an initial value`() {
        val statements = listOf(VariableDeclaration("x", "number", null))

        assertEquals("let x: number;\n", format(statements))
    }

    @Test
    fun `formats an assignment`() {
        val statements = listOf(Assignment("x", NumberLiteral(10.0)))

        assertEquals("x = 10.0;\n", format(statements))
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
    fun `formats a binary expression with one space before and after the operator`() {
        val expr = BinaryExpression(Identifier("x"), TokenType.PLUS, NumberLiteral(3.0))
        val statements = listOf(Assignment("y", expr))

        assertEquals("y = x + 3.0;\n", format(statements))
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

            assertEquals("y = 1.0 $symbol 2.0;\n", format(statements))
        }
    }

    @Test
    fun `fails when the operator is not one of the supported ones`() {
        val expr = BinaryExpression(NumberLiteral(1.0), TokenType.ASSIGN, NumberLiteral(2.0))
        val statements = listOf(Assignment("y", expr))

        assertFailsWith<IllegalStateException> { format(statements) }
    }

    @Test
    fun `adds a space before the colon when the rule is on`() {
        val statements = listOf(VariableDeclaration("x", "number", null))
        val rules = FormatterRules(spaceBeforeColon = true)

        assertEquals("let x : number;\n", format(statements, rules))
    }

    @Test
    fun `does not add a space after the colon when the rule is off`() {
        val statements = listOf(VariableDeclaration("x", "number", null))
        val rules = FormatterRules(spaceAfterColon = false)

        assertEquals("let x:number;\n", format(statements, rules))
    }

    @Test
    fun `does not add spaces around the equals sign in a declaration when the rule is off`() {
        val statements = listOf(VariableDeclaration("x", "number", NumberLiteral(5.0)))
        val rules = FormatterRules(spaceAroundAssignment = false)

        assertEquals("let x: number=5.0;\n", format(statements, rules))
    }

    @Test
    fun `does not add spaces around the equals sign in an assignment when the rule is off`() {
        val statements = listOf(Assignment("x", NumberLiteral(5.0)))
        val rules = FormatterRules(spaceAroundAssignment = false)

        assertEquals("x=5.0;\n", format(statements, rules))
    }

    @Test
    fun `does not add a line break before println when the rule is 0`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                PrintCall(Identifier("x")),
            )
        val rules = FormatterRules(lineBreaksBeforePrintln = 0)

        assertEquals("let x: number = 1.0;println(x);\n", format(statements, rules))
    }

    @Test
    fun `adds a single line break before println by default`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                PrintCall(Identifier("x")),
            )

        assertEquals("let x: number = 1.0;\nprintln(x);\n", format(statements))
    }

    @Test
    fun `adds two line breaks before println when the rule is 2`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                PrintCall(Identifier("x")),
            )
        val rules = FormatterRules(lineBreaksBeforePrintln = 2)

        assertEquals("let x: number = 1.0;\n\nprintln(x);\n", format(statements, rules))
    }

    @Test
    fun `the println rule does not affect the first statement in the list`() {
        val statements = listOf(PrintCall(NumberLiteral(1.0)))
        val rules = FormatterRules(lineBreaksBeforePrintln = 2)

        assertEquals("println(1.0);\n", format(statements, rules))
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
            "let x: number = 5.0;\n" +
                "x = x + 1.0;\n" +
                "println(x);\n" +
                "let greeting: string = \"hello\";\n" +
                "println(greeting);\n"

        assertEquals(expected, format(statements))
    }
}
