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
import printscript.interpreter.output.BucketOutput
import printscript.interpreter.output.MultiOutput
import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InterpreterTest {
    private fun run(vararg statements: Statement): List<String> {
        val bucket = BucketOutput()
        Interpreter(bucket).interpret(statements.iterator())
        return bucket.lines()
    }

    private fun num(value: Double) = NumberLiteral(value)

    private fun text(value: String) = StringLiteral(value)

    private fun id(name: String) = Identifier(name)

    private fun bin(
        left: Expression,
        op: TokenType,
        right: Expression,
    ) = BinaryExpression(left, op, right)

    @Test
    fun `concatenation of two string variables`() {
        // let name: string = "Joe";
        // let lastName: string = "Doe";
        // println(name + " " + lastName);
        val output =
            run(
                VariableDeclaration("name", "string", text("Joe")),
                VariableDeclaration("lastName", "string", text("Doe")),
                PrintCall(
                    bin(
                        bin(id("name"), TokenType.PLUS, text(" ")),
                        TokenType.PLUS,
                        id("lastName"),
                    ),
                ),
            )

        assertEquals(listOf("Joe Doe"), output)
    }

    @Test
    fun `division stored in a variable and concatenated`() {
        // let a: number = 12;
        // let b: number = 4;
        // let c: number = a / b;
        // println("Result: " + c);
        val output =
            run(
                VariableDeclaration("a", "number", num(12.0)),
                VariableDeclaration("b", "number", num(4.0)),
                VariableDeclaration("c", "number", bin(id("a"), TokenType.DIVIDE, id("b"))),
                PrintCall(bin(text("Result: "), TokenType.PLUS, id("c"))),
            )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `reassignment of an already declared variable`() {
        // let a: number = 12;
        // let b: number = 4;
        // a = a / b;
        // println("Result: " + a);
        val output =
            run(
                VariableDeclaration("a", "number", num(12.0)),
                VariableDeclaration("b", "number", num(4.0)),
                Assignment("a", bin(id("a"), TokenType.DIVIDE, id("b"))),
                PrintCall(bin(text("Result: "), TokenType.PLUS, id("a"))),
            )

        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `using an undeclared variable is an error`() {
        val error =
            assertFailsWith<UndeclaredVariableError> {
                run(PrintCall(id("x")))
            }

        assertEquals("x", error.name)
    }

    @Test
    fun `subtracting on a string is a type error`() {
        val error =
            assertFailsWith<TypeMismatchError> {
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

    @Test
    fun `printing with a MultiOutput reaches every destination`() {
        // println(5 * 3);
        val first = BucketOutput()
        val second = BucketOutput()

        Interpreter(MultiOutput(first, second)).interpret(
            listOf(PrintCall(bin(num(5.0), TokenType.MULTIPLY, num(3.0)))).iterator()
        )

        assertEquals(listOf("15"), first.lines())
        assertEquals(listOf("15"), second.lines())
    }

    @Test
    fun `a statement with no interpreter registered fails`() {
        val interpreter = Interpreter(emptyList(), emptyList())

        assertFailsWith<UnknownStatementError> {
            interpreter.interpret(listOf(PrintCall(text("line"))).iterator())
        }
    }

    @Test
    fun `an expression with no evaluator registered fails`() {
        val interpreter = Interpreter(
            statementInterpreters = listOf(PrintCallInterpreter(BucketOutput())),
            expressionEvaluators = emptyList()
        )

        assertFailsWith<UnknownExpressionError> {
            interpreter.interpret(listOf(PrintCall(text("line"))).iterator())
        }
    }

    //cada plugin tiene un guard tira error si le llega un nodo q no es suyo
    //por el flujo normal nunca pasa, pq el Interpreter pregunta matches() antes

    @Test
    fun `statement plugins reject nodes that are not theirs`() {
        val print = PrintCall(text("hello"))
        val assignment = Assignment("x", num(1.0))

        val cases = listOf(
            VariableDeclarationInterpreter() to print,
            AssignmentInterpreter() to print,
            PrintCallInterpreter(BucketOutput()) to assignment
        )

        for ((plugin, foreignNode) in cases) {
            assertFailsWith<UnknownStatementError> {
                plugin.execute(foreignNode, Environment(), Interpreter())
            }
        }
    }

    @Test
    fun `expression plugins reject nodes that are not theirs`() {
        val number = num(1.0)
        val string = text("hello")

        val cases = listOf(
            NumberLiteralEvaluator() to string,
            StringLiteralEvaluator() to number,
            IdentifierEvaluator() to number,
            BinaryExpressionEvaluator() to number
        )

        for ((plugin, foreignNode) in cases) {
            assertFailsWith<UnknownExpressionError> {
                plugin.evaluate(foreignNode, Environment(), Interpreter())
            }
        }
    }
}
