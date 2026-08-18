package printscript.interpreter

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Expression
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

class InterpreterTest {

    private fun run(vararg statements: Statement): List<String> {
        val output = mutableListOf<String>()
        Interpreter { text -> output.add(text) }.interpret(statements.iterator())
        return output
    }

    private fun num(value: Double) = NumberLiteral(value)

    private fun text(value: String) = StringLiteral(value)

    private fun id(name: String) = Identifier(name)

    private fun bin(left: Expression, op: TokenType, right: Expression) = BinaryExpression(left, op, right)

    @Test
    fun `example 1 - concatenation of two string variables`() {
        // let name: string = "Joe";
        // let lastName: string = "Doe";
        // println(name + " " + lastName);
        val output = run(
            VariableDeclaration("name", "string", text("Joe")),
            VariableDeclaration("lastName", "string", text("Doe")),
            PrintCall(
                bin(
                    bin(id("name"), TokenType.PLUS, text(" ")),
                    TokenType.PLUS,
                    id("lastName")
                )
            )
        )

        assertEquals(listOf("Joe Doe"), output)
    }

    @Test
    fun `example 2 - division stored in a variable and concatenated`() {
        // let a: number = 12;
        // let b: number = 4;
        // let c: number = a / b;
        // println("Result: " + c);
        val output = run(
            VariableDeclaration("a", "number", num(12.0)),
            VariableDeclaration("b", "number", num(4.0)),
            VariableDeclaration("c", "number", bin(id("a"), TokenType.DIVIDE, id("b"))),
            PrintCall(bin(text("Result: "), TokenType.PLUS, id("c")))
        )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `example 3 - reassignment of an already declared variable`() {
        // let a: number = 12;
        // let b: number = 4;
        // a = a / b;
        // println("Result: " + a);
        val output = run(
            VariableDeclaration("a", "number", num(12.0)),
            VariableDeclaration("b", "number", num(4.0)),
            Assignment("a", bin(id("a"), TokenType.DIVIDE, id("b"))),
            PrintCall(bin(text("Result: "), TokenType.PLUS, id("a")))
        )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `using an undeclared variable is an error`() {
        val error = assertFailsWith<UndeclaredVariableError> {
            run(PrintCall(id("x")))
        }

        assertEquals("x", error.name)
    }

    @Test
    fun `subtracting on a string is a type error`() {
        val error = assertFailsWith<TypeMismatchError> {
            run(PrintCall(bin(text("hola"), TokenType.MINUS, num(1.0))))
        }

        assertEquals("string", error.leftType)
        assertEquals("number", error.rightType)
    }

    @Test
    fun `adding two numbers gives a number and does not concatenate them`() {
        val output = run(PrintCall(bin(num(1.0), TokenType.PLUS, num(2.0))))

        assertEquals(listOf("3"), output)
    }

    @Test
    fun `a result with decimals keeps the decimals`() {
        val output = run(PrintCall(bin(num(7.0), TokenType.DIVIDE, num(2.0))))

        assertEquals(listOf("3.5"), output)
    }
}


