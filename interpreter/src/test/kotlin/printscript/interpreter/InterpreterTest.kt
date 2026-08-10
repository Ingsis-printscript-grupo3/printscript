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

    private fun correr(vararg statements: Statement): List<String> {
        val salida = mutableListOf<String>()
        Interpreter { texto -> salida.add(texto) }.interpretar(statements.toList())
        return salida
    }

    private fun num(valor: Double) = NumberLiteral(valor)

    private fun texto(valor: String) = StringLiteral(valor)

    private fun id(nombre: String) = Identifier(nombre)

    private fun bin(izq: Expression, op: TokenType, der: Expression) = BinaryExpression(izq, op, der)

    @Test
    fun `ejemplo 1 - concatenacion de dos variables string`() {
        // let name: string = "Joe";
        // let lastName: string = "Doe";
        // println(name + " " + lastName);
        val salida = correr(
            VariableDeclaration("name", "string", texto("Joe")),
            VariableDeclaration("lastName", "string", texto("Doe")),
            PrintCall(
                bin(
                    bin(id("name"), TokenType.PLUS, texto(" ")),
                    TokenType.PLUS,
                    id("lastName")
                )
            )
        )

        assertEquals(listOf("Joe Doe"), salida)
    }

    @Test
    fun `ejemplo 2 - division guardada en una variable y concatenada`() {
        // let a: number = 12;
        // let b: number = 4;
        // let c: number = a / b;
        // println("Result: " + c);
        val salida = correr(
            VariableDeclaration("a", "number", num(12.0)),
            VariableDeclaration("b", "number", num(4.0)),
            VariableDeclaration("c", "number", bin(id("a"), TokenType.DIVIDE, id("b"))),
            PrintCall(bin(texto("Result: "), TokenType.PLUS, id("c")))
        )

        assertEquals(listOf("Result: 3"), salida)
    }

    @Test
    fun `ejemplo 3 - reasignacion de una variable ya declarada`() {
        // let a: number = 12;
        // let b: number = 4;
        // a = a / b;
        // println("Result: " + a);
        val salida = correr(
            VariableDeclaration("a", "number", num(12.0)),
            VariableDeclaration("b", "number", num(4.0)),
            Assignment("a", bin(id("a"), TokenType.DIVIDE, id("b"))),
            PrintCall(bin(texto("Result: "), TokenType.PLUS, id("a")))
        )

        assertEquals(listOf("Result: 3"), salida)
    }

    @Test
    fun `usar una variable no declarada es error`() {
        val error = assertFailsWith<UndeclaredVariableError> {
            correr(PrintCall(id("x")))
        }

        assertEquals("x", error.name)
    }

    @Test
    fun `restar sobre un string es error de tipos`() {
        val error = assertFailsWith<TypeMismatchError> {
            correr(PrintCall(bin(texto("hola"), TokenType.MINUS, num(1.0))))
        }

        assertEquals("string", error.leftType)
        assertEquals("number", error.rightType)
    }

    @Test
    fun `sumar dos numbers da un number y no los concatena`() {
        val salida = correr(PrintCall(bin(num(1.0), TokenType.PLUS, num(2.0))))

        assertEquals(listOf("3"), salida)
    }

    @Test
    fun `un resultado con decimales conserva los decimales`() {
        val salida = correr(PrintCall(bin(num(7.0), TokenType.DIVIDE, num(2.0))))

        assertEquals(listOf("3.5"), salida)
    }
}
