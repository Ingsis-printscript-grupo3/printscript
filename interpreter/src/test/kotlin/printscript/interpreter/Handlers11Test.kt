package printscript.interpreter

import printscript.ast.Assignment
import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.Identifier
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.BucketOutput
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.plugin.expression.BooleanLiteralEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.ReadEnvEvaluator
import printscript.interpreter.plugin.expression.ReadInputEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import printscript.interpreter.plugin.statement.Assignment11Interpreter
import printscript.interpreter.plugin.statement.BlockInterpreter
import printscript.interpreter.plugin.statement.ConstDeclarationInterpreter
import printscript.interpreter.plugin.statement.IfStatementInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclaration11Interpreter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Handlers11Test {
    private fun createTestInterpreter(
        output: BucketOutput = BucketOutput(),
        input: QueueInput = QueueInput(),
        env: MapEnvProvider = MapEnvProvider(),
    ): Interpreter {
        val statements =
            listOf(
                VariableDeclaration11Interpreter(),
                ConstDeclarationInterpreter(),
                Assignment11Interpreter(),
                PrintCallInterpreter(output),
                IfStatementInterpreter(),
                BlockInterpreter(),
            )
        val expressions =
            listOf(
                BooleanLiteralEvaluator(),
                NumberLiteralEvaluator(),
                StringLiteralEvaluator(),
                ReadInputEvaluator(input),
                ReadEnvEvaluator(env),
            )
        return Interpreter(statements, expressions)
    }

    @Test
    fun `BooleanLiteralEvaluator evaluates boolean literals`() {
        val evaluator = BooleanLiteralEvaluator()
        val ctx = InterpreterContext(Environment(), Interpreter())

        assertEquals(BooleanValue(true), evaluator.handle(BooleanLiteral(true), ctx))
        assertEquals(BooleanValue(false), evaluator.handle(BooleanLiteral(false), ctx))
        assertTrue(evaluator.applies(BooleanLiteral(true)))
        assertFalse(evaluator.applies(StringLiteral("hi")))

        assertFailsWith<UnknownExpressionError> {
            evaluator.handle(StringLiteral("hi"), ctx)
        }
    }

    @Test
    fun `ReadInputEvaluator reads from input provider`() {
        val input = QueueInput("user_input")
        val evaluator = ReadInputEvaluator(input)
        val dummyInterpreter =
            object : InterpreterInterface {
                override fun interpret(statements: Iterator<printscript.ast.Statement>) {}

                override fun evaluate(expression: printscript.ast.Expression): Value = StringValue("prompt: ")
            }
        val ctx = InterpreterContext(Environment(), dummyInterpreter)

        val result = evaluator.handle(ReadInput(StringLiteral("prompt: ")), ctx)
        assertEquals(StringValue("user_input"), result)
        assertTrue(evaluator.applies(ReadInput(StringLiteral("p"))))
        assertFalse(evaluator.applies(StringLiteral("p")))

        assertFailsWith<UnknownExpressionError> {
            evaluator.handle(StringLiteral("p"), ctx)
        }
    }

    @Test
    fun `ReadEnvEvaluator reads existing environment variables`() {
        val env = MapEnvProvider("MY_VAR" to "my_value")
        val evaluator = ReadEnvEvaluator(env)
        val dummyInterpreter =
            object : InterpreterInterface {
                override fun interpret(statements: Iterator<printscript.ast.Statement>) {}

                override fun evaluate(expression: printscript.ast.Expression): Value = StringValue("MY_VAR")
            }
        val ctx = InterpreterContext(Environment(), dummyInterpreter)

        val result = evaluator.handle(ReadEnv(StringLiteral("MY_VAR")), ctx)
        assertEquals(StringValue("my_value"), result)
        assertTrue(evaluator.applies(ReadEnv(StringLiteral("x"))))
        assertFalse(evaluator.applies(StringLiteral("x")))

        assertFailsWith<UnknownExpressionError> {
            evaluator.handle(StringLiteral("x"), ctx)
        }
    }

    @Test
    fun `ReadEnvEvaluator throws EnvVariableNotFoundError if variable does not exist`() {
        val env = MapEnvProvider()
        val evaluator = ReadEnvEvaluator(env)
        val dummyInterpreter =
            object : InterpreterInterface {
                override fun interpret(statements: Iterator<printscript.ast.Statement>) {}

                override fun evaluate(expression: printscript.ast.Expression): Value = StringValue("UNKNOWN_VAR")
            }
        val ctx = InterpreterContext(Environment(), dummyInterpreter)

        val error =
            assertFailsWith<EnvVariableNotFoundError> {
                evaluator.handle(ReadEnv(StringLiteral("UNKNOWN_VAR")), ctx)
            }
        assertEquals("UNKNOWN_VAR", error.name)
    }

    @Test
    fun `IfStatementInterpreter executes thenBranch when condition is true`() {
        val output = BucketOutput()
        val stmt =
            IfStatement(
                condition = BooleanLiteral(true),
                thenBranch = Block(listOf(PrintCall(StringLiteral("inside then")))),
                elseBranch = Block(listOf(PrintCall(StringLiteral("inside else")))),
            )

        val interpreter = createTestInterpreter(output = output)
        interpreter.interpret(stmt)

        assertEquals(listOf("inside then"), output.lines())
    }

    @Test
    fun `IfStatementInterpreter executes elseBranch when condition is false`() {
        val output = BucketOutput()
        val stmt =
            IfStatement(
                condition = BooleanLiteral(false),
                thenBranch = Block(listOf(PrintCall(StringLiteral("inside then")))),
                elseBranch = Block(listOf(PrintCall(StringLiteral("inside else")))),
            )

        val interpreter = createTestInterpreter(output = output)
        interpreter.interpret(stmt)

        assertEquals(listOf("inside else"), output.lines())
    }

    @Test
    fun `IfStatementInterpreter does nothing when condition is false and elseBranch is null`() {
        val output = BucketOutput()
        val stmt =
            IfStatement(
                condition = BooleanLiteral(false),
                thenBranch = Block(listOf(PrintCall(StringLiteral("inside then")))),
                elseBranch = null,
            )

        val interpreter = createTestInterpreter(output = output)
        interpreter.interpret(stmt)

        assertTrue(output.lines().isEmpty())
    }

    @Test
    fun `IfStatementInterpreter throws ConditionTypeError if condition is not boolean`() {
        val interpreter =
            Interpreter(
                statementInterpreters = listOf(IfStatementInterpreter()),
                expressionEvaluators =
                    listOf(
                        printscript.interpreter.plugin.expression.NumberLiteralEvaluator(),
                    ),
            )

        val stmt =
            IfStatement(
                condition = NumberLiteral(1.0),
                thenBranch = Block(emptyList()),
                elseBranch = null,
            )

        val error =
            assertFailsWith<ConditionTypeError> {
                interpreter.interpret(stmt)
            }
        assertEquals("number", error.actualType)
    }

    @Test
    fun `IfStatementInterpreter rejects foreign nodes`() {
        val interpreter = IfStatementInterpreter()
        val ctx = InterpreterContext(Environment(), Interpreter())

        assertTrue(interpreter.applies(IfStatement(BooleanLiteral(true), Block(emptyList()), null)))
        assertFalse(interpreter.applies(Block(emptyList())))

        assertFailsWith<UnknownStatementError> {
            interpreter.handle(Block(emptyList()), ctx)
        }
    }

    @Test
    fun `BlockInterpreter enters and exits scope`() {
        val blockHandler = BlockInterpreter()
        val ctx = InterpreterContext(Environment(), Interpreter())

        assertTrue(blockHandler.applies(Block(emptyList())))
        assertFalse(blockHandler.applies(PrintCall(StringLiteral("hi"))))

        assertFailsWith<UnknownStatementError> {
            blockHandler.handle(PrintCall(StringLiteral("hi")), ctx)
        }
    }

    @Test
    fun `ValueConverter converts readInput to number and boolean`() {
        val inputExpr = ReadInput(StringLiteral("prompt"))

        val numResult = ValueConverter.convert(StringValue("42.5"), "number", inputExpr)
        assertEquals(NumberValue(42.5), numResult)

        val boolTrueResult = ValueConverter.convert(StringValue("true"), "boolean", inputExpr)
        assertEquals(BooleanValue(true), boolTrueResult)

        val boolFalseResult = ValueConverter.convert(StringValue("false"), "boolean", inputExpr)
        assertEquals(BooleanValue(false), boolFalseResult)

        val strResult = ValueConverter.convert(StringValue("hello"), "string", inputExpr)
        assertEquals(StringValue("hello"), strResult)

        val numError =
            assertFailsWith<ReadInputConversionError> {
                ValueConverter.convert(StringValue("not_a_number"), "number", inputExpr)
            }
        assertEquals("not_a_number", numError.value)
        assertEquals("number", numError.targetType)

        val boolError =
            assertFailsWith<ReadInputConversionError> {
                ValueConverter.convert(StringValue("hello"), "boolean", inputExpr)
            }
        assertEquals("hello", boolError.value)
        assertEquals("boolean", boolError.targetType)

        val unsupportedError =
            assertFailsWith<ReadInputConversionError> {
                ValueConverter.convert(StringValue("val"), "custom_type", inputExpr)
            }
        assertEquals("val", unsupportedError.value)
        assertEquals("custom_type", unsupportedError.targetType)
    }

    @Test
    fun `ValueConverter converts readEnv to number and boolean`() {
        val envExpr = ReadEnv(StringLiteral("MY_ENV"))

        val numResult = ValueConverter.convert(StringValue("100"), "number", envExpr)
        assertEquals(NumberValue(100.0), numResult)

        val boolResult = ValueConverter.convert(StringValue("true"), "boolean", envExpr)
        assertEquals(BooleanValue(true), boolResult)

        val boolFalseResult = ValueConverter.convert(StringValue("false"), "boolean", envExpr)
        assertEquals(BooleanValue(false), boolFalseResult)

        val numError =
            assertFailsWith<ReadEnvConversionError> {
                ValueConverter.convert(StringValue("bad"), "number", envExpr)
            }
        assertEquals("StringLiteral(value=MY_ENV, position=Position(line=0, column=0))", numError.name)
        assertEquals("bad", numError.value)

        val boolError =
            assertFailsWith<ReadEnvConversionError> {
                ValueConverter.convert(StringValue("bad"), "boolean", envExpr)
            }
        assertEquals("bad", boolError.value)

        val unsupportedError =
            assertFailsWith<ReadEnvConversionError> {
                ValueConverter.convert(StringValue("bad"), "custom", envExpr)
            }
        assertEquals("custom", unsupportedError.targetType)
    }

    @Test
    fun `ValueConverter throws TypeMismatchError on invalid plain types`() {
        assertFailsWith<TypeMismatchError> {
            ValueConverter.convert(NumberValue(1.0), "string")
        }
        assertFailsWith<TypeMismatchError> {
            ValueConverter.convert(StringValue("hello"), "number")
        }
    }

    @Test
    fun `VariableDeclaration and Assignment with const and conversion`() {
        val env = Environment()
        val dummyInterpreter =
            object : InterpreterInterface {
                override fun interpret(statements: Iterator<printscript.ast.Statement>) {}

                override fun evaluate(expression: printscript.ast.Expression): Value =
                    when (expression) {
                        is StringLiteral -> StringValue(expression.value)
                        is NumberLiteral -> NumberValue(expression.value)
                        is BooleanLiteral -> BooleanValue(expression.value)
                        is ReadInput -> StringValue("99")
                        is ReadEnv -> StringValue("true")
                        is Identifier -> env.lookup(expression.name)
                        else -> throw IllegalArgumentException()
                    }
            }
        val ctx = InterpreterContext(env, dummyInterpreter)
        val varDecl = VariableDeclaration11Interpreter()
        val constDecl = ConstDeclarationInterpreter()
        val assign = Assignment11Interpreter()

        varDecl.handle(
            VariableDeclaration("inputNum", "number", ReadInput(StringLiteral("p"))),
            ctx,
        )
        assertEquals(NumberValue(99.0), env.lookup("inputNum"))

        varDecl.handle(
            VariableDeclaration("envBool", "boolean", ReadEnv(StringLiteral("B"))),
            ctx,
        )
        assertEquals(BooleanValue(true), env.lookup("envBool"))

        constDecl.handle(
            VariableDeclaration("c", "number", NumberLiteral(10.0), isConst = true),
            ctx,
        )
        assertEquals(NumberValue(10.0), env.lookup("c"))

        assertFailsWith<CannotAssignToConstError> {
            assign.handle(Assignment("c", NumberLiteral(20.0)), ctx)
        }

        varDecl.handle(
            VariableDeclaration("mutableNum", "number", NumberLiteral(1.0)),
            ctx,
        )
        assign.handle(Assignment("mutableNum", ReadInput(StringLiteral("p"))), ctx)
        assertEquals(NumberValue(99.0), env.lookup("mutableNum"))
    }

    @Test
    fun `1_1 statement plugins reject foreign nodes`() {
        val varDecl = VariableDeclaration11Interpreter()
        val constDecl = ConstDeclarationInterpreter()
        val assign = Assignment11Interpreter()
        val ctx = InterpreterContext(Environment(), Interpreter())

        val letNode = VariableDeclaration("x", "number", NumberLiteral(1.0), isConst = false)
        val constNode = VariableDeclaration("y", "number", NumberLiteral(2.0), isConst = true)
        val assignNode = Assignment("x", NumberLiteral(3.0))

        assertTrue(varDecl.applies(letNode))
        assertFalse(varDecl.applies(constNode))
        assertFalse(varDecl.applies(assignNode))

        assertTrue(constDecl.applies(constNode))
        assertFalse(constDecl.applies(letNode))
        assertFalse(constDecl.applies(assignNode))

        assertTrue(assign.applies(assignNode))
        assertFalse(assign.applies(letNode))

        assertFailsWith<UnknownStatementError> { varDecl.handle(constNode, ctx) }
        assertFailsWith<UnknownStatementError> { varDecl.handle(assignNode, ctx) }
        assertFailsWith<UnknownStatementError> { constDecl.handle(letNode, ctx) }
        assertFailsWith<UnknownStatementError> { constDecl.handle(assignNode, ctx) }
        assertFailsWith<UnknownStatementError> { assign.handle(letNode, ctx) }
    }
}
