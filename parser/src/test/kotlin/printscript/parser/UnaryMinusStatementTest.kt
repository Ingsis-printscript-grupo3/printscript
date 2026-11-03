package printscript.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.TokenType
import kotlin.test.assertEquals

class UnaryMinusStatementTest {
    @Test
    fun `parses a variable declaration with a negative number in version 1_0`() {
        val statements = parseStatements("let x: number = -5;", LanguageVersion.V1_0)

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(-5.0, value.value)
    }

    @Test
    fun `parses a const declaration with a negative number`() {
        val statements = parseStatements("const x: number = -42;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(-42.0, value.value)
        assert(stmt.isConst)
    }

    @Test
    fun `parses an assignment statement with a negative number`() {
        val statements = parseStatements("x = -15;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as Assignment
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(-15.0, value.value)
    }

    @Test
    fun `parses an assignment statement with a negative identifier`() {
        val statements = parseStatements("x = -y;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as Assignment
        val binary = stmt.value as BinaryExpression
        assertEquals(TokenType.MINUS, binary.operator)
        assertEquals(0.0, (binary.left as NumberLiteral).value)
        assertEquals("y", (binary.right as Identifier).name)
    }

    @Test
    fun `parses a variable declaration with a parenthesized negative expression`() {
        val statements = parseStatements("let x: number = -(a + b);")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        val binary = stmt.value as BinaryExpression
        assertEquals(TokenType.MINUS, binary.operator)
        assertEquals(0.0, (binary.left as NumberLiteral).value)
        val right = binary.right as BinaryExpression
        assertEquals(TokenType.PLUS, right.operator)
        assertEquals("a", (right.left as Identifier).name)
        assertEquals("b", (right.right as Identifier).name)
    }

    @Test
    fun `fails when assignment ends with trailing unary minus`() {
        val exception = assertThrows<SyntaxError> { parseStatements("x = -;") }

        assert(exception.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails when const declaration ends with trailing unary minus`() {
        val exception = assertThrows<SyntaxError> { parseStatements("const x: number = -;") }

        assert(exception.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails when statement contains unary minus followed by multiplication`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let x: number = - * 5;") }

        assert(exception.message.contains("Expected a value or expression"))
    }
}
