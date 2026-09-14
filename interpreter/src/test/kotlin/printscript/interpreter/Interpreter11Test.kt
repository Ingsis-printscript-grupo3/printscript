package printscript.interpreter

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.TokenType
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class Interpreter11Test {
    private fun num(value: Double) = NumberLiteral(value)

    private fun text(value: String) = StringLiteral(value)

    private fun bool(value: Boolean) = BooleanLiteral(value)

    private fun id(name: String) = Identifier(name)

    private fun bin(
        left: Expression,
        op: TokenType,
        right: Expression,
    ) = BinaryExpression(left, op, right)

    @Test
    fun `factory creates 1_0 and 1_1 interpreters`() {
        val interp10 = InterpreterFactory.create("1.0")
        val interp11 = InterpreterFactory.create("1.1")
        val interp10Enum = InterpreterFactory.create(printscript.common.LanguageVersion.V1_0)
        val interp11Enum = InterpreterFactory.create(printscript.common.LanguageVersion.V1_1)

        assertNotNull(interp10)
        assertNotNull(interp11)
        assertNotNull(interp10Enum)
        assertNotNull(interp11Enum)

        assertFailsWith<IllegalArgumentException> {
            InterpreterFactory.create("2.0")
        }
    }

    @Test
    fun `1_0 interpreter rejects 1_1 features`() {
        val output = BucketOutput()
        val interp10 = InterpreterFactory.create10(output)

        // 1.0 rejects IfStatement
        assertFailsWith<UnknownStatementError> {
            interp10.interpret(
                listOf<Statement>(
                    IfStatement(bool(true), Block(emptyList()), null),
                ).iterator(),
            )
        }

        // 1.0 rejects boolean literal
        assertFailsWith<UnknownExpressionError> {
            interp10.evaluate(bool(true))
        }

        // 1.0 rejects readInput
        assertFailsWith<UnknownExpressionError> {
            interp10.evaluate(ReadInput(text("prompt")))
        }

        // 1.0 rejects readEnv
        assertFailsWith<UnknownExpressionError> {
            interp10.evaluate(ReadEnv(text("MY_VAR")))
        }
    }

    @Test
    fun `const declaration and lookup works in 1_1`() {
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        // const x: number = 42;
        // println(x);
        interpreter.interpret(
            listOf(
                VariableDeclaration("x", "number", num(42.0), isConst = true),
                PrintCall(id("x")),
            ).iterator(),
        )

        assertEquals(listOf("42"), output.lines())
    }

    @Test
    fun `const reassignment throws CannotAssignToConstError`() {
        val interpreter = InterpreterFactory.create11()

        val error =
            assertFailsWith<CannotAssignToConstError> {
                interpreter.interpret(
                    listOf(
                        VariableDeclaration("PI", "number", num(3.14), isConst = true),
                        Assignment("PI", num(3.14159)),
                    ).iterator(),
                )
            }

        assertEquals("PI", error.name)
    }

    @Test
    fun `readInput converts to number and can be used in arithmetic`() {
        val output = BucketOutput()
        val input = QueueInput("25")
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        // let age: number = readInput("Enter age: ");
        // let nextAge: number = age + 1;
        // println(nextAge);
        interpreter.interpret(
            listOf(
                VariableDeclaration("age", "number", ReadInput(text("Enter age: "))),
                VariableDeclaration("nextAge", "number", bin(id("age"), TokenType.PLUS, num(1.0))),
                PrintCall(id("nextAge")),
            ).iterator(),
        )

        assertEquals(listOf("26"), output.lines())
    }

    @Test
    fun `readInput converts to boolean`() {
        val output = BucketOutput()
        val input = QueueInput("true")
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        // let active: boolean = readInput("Is active: ");
        // println(active);
        interpreter.interpret(
            listOf(
                VariableDeclaration("active", "boolean", ReadInput(text("Is active: "))),
                PrintCall(id("active")),
            ).iterator(),
        )

        assertEquals(listOf("true"), output.lines())
    }

    @Test
    fun `readInput invalid number conversion fails with ValueConversionError`() {
        val input = QueueInput("not_a_number")
        val interpreter = InterpreterFactory.create11(input = input)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(
                    listOf(
                        VariableDeclaration("n", "number", ReadInput(text("Enter: "))),
                    ).iterator(),
                )
            }

        assertEquals(StringValue("not_a_number"), error.value)
        assertEquals("number", error.targetType)
    }

    @Test
    fun `readEnv reads environment variable and converts to number`() {
        val output = BucketOutput()
        val env = MapEnvProvider("PORT" to "8080")
        val interpreter = InterpreterFactory.create11(output = output, env = env)

        // let port: number = readEnv("PORT");
        // println(port);
        interpreter.interpret(
            listOf(
                VariableDeclaration("port", "number", ReadEnv(text("PORT"))),
                PrintCall(id("port")),
            ).iterator(),
        )

        assertEquals(listOf("8080"), output.lines())
    }

    @Test
    fun `readEnv missing variable throws EnvVariableNotFoundError`() {
        val interpreter = InterpreterFactory.create11(env = MapEnvProvider())

        val error =
            assertFailsWith<EnvVariableNotFoundError> {
                interpreter.interpret(
                    listOf(
                        VariableDeclaration("secret", "string", ReadEnv(text("SECRET_KEY"))),
                    ).iterator(),
                )
            }

        assertEquals("SECRET_KEY", error.name)
    }

    @Test
    fun `readEnv invalid boolean conversion throws ValueConversionError`() {
        val env = MapEnvProvider("DEBUG" to "maybe")
        val interpreter = InterpreterFactory.create11(env = env)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(
                    listOf(
                        VariableDeclaration("debug", "boolean", ReadEnv(text("DEBUG"))),
                    ).iterator(),
                )
            }

        assertEquals(StringValue("maybe"), error.value)
        assertEquals("boolean", error.targetType)
    }

    @Test
    fun `if statement executes thenBranch and scopes inner variables`() {
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        // let x: number = 10;
        // if (true) {
        //     let y: number = 20;
        //     x = x + y;
        //     println("inside: " + x);
        // }
        // println("outside: " + x);
        val program =
            listOf(
                VariableDeclaration("x", "number", num(10.0)),
                IfStatement(
                    condition = bool(true),
                    thenBranch =
                        Block(
                            listOf(
                                VariableDeclaration("y", "number", num(20.0)),
                                Assignment("x", bin(id("x"), TokenType.PLUS, id("y"))),
                                PrintCall(bin(text("inside: "), TokenType.PLUS, id("x"))),
                            ),
                        ),
                    elseBranch = null,
                ),
                PrintCall(bin(text("outside: "), TokenType.PLUS, id("x"))),
            )

        interpreter.interpret(program.iterator())

        assertEquals(listOf("inside: 30", "outside: 30"), output.lines())

        // y was declared inside the block, cannot be accessed outside
        assertFailsWith<UndeclaredVariableError> {
            interpreter.interpret(listOf(PrintCall(id("y"))).iterator())
        }
    }

    @Test
    fun `if statement executes elseBranch when false`() {
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        // if (false) {
        //     println("then");
        // } else {
        //     println("else");
        // }
        val program =
            listOf(
                IfStatement(
                    condition = bool(false),
                    thenBranch = Block(listOf(PrintCall(text("then")))),
                    elseBranch = Block(listOf(PrintCall(text("else")))),
                ),
            )

        interpreter.interpret(program.iterator())

        assertEquals(listOf("else"), output.lines())
    }

    @Test
    fun `variable shadowing inside if block does not overwrite outer variable`() {
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        // let a: string = "outer";
        // if (true) {
        //     let a: string = "inner";
        //     println(a);
        // }
        // println(a);
        val program =
            listOf(
                VariableDeclaration("a", "string", text("outer")),
                IfStatement(
                    condition = bool(true),
                    thenBranch =
                        Block(
                            listOf(
                                VariableDeclaration("a", "string", text("inner")),
                                PrintCall(id("a")),
                            ),
                        ),
                    elseBranch = null,
                ),
                PrintCall(id("a")),
            )

        interpreter.interpret(program.iterator())

        assertEquals(listOf("inner", "outer"), output.lines())
    }

    @Test
    fun `assignment with type conversion from readInput works`() {
        val output = BucketOutput()
        val input = QueueInput("50")
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        interpreter.interpret(
            listOf(
                VariableDeclaration("x", "number", num(10.0)),
                Assignment("x", ReadInput(text("Enter: "))),
                PrintCall(id("x")),
            ).iterator(),
        )

        assertEquals(listOf("50"), output.lines())
    }

    @Test
    fun `assignment with invalid conversion throws ValueConversionError`() {
        val input = QueueInput("not_a_number")
        val interpreter = InterpreterFactory.create11(input = input)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(
                    listOf(
                        VariableDeclaration("x", "number", num(10.0)),
                        Assignment("x", ReadInput(text("Enter: "))),
                    ).iterator(),
                )
            }

        assertEquals(StringValue("not_a_number"), error.value)
        assertEquals("number", error.targetType)
    }

    @Test
    fun `declaration without initializer allows subsequent assignment`() {
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(
            listOf(
                VariableDeclaration("x", "number", null),
                Assignment("x", num(42.0)),
                PrintCall(id("x")),
            ).iterator(),
        )

        assertEquals(listOf("42"), output.lines())
    }

    @Test
    fun `accessing uninitialized variable throws UninitializedVariableError`() {
        val interpreter = InterpreterFactory.create11()

        val error =
            assertFailsWith<UninitializedVariableError> {
                interpreter.interpret(
                    listOf(
                        VariableDeclaration("x", "number", null),
                        PrintCall(id("x")),
                    ).iterator(),
                )
            }

        assertEquals("x", error.name)
    }
}
