package printscript.interpreter

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.BucketOutput
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerFactory
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class Interpreter11Test {
    @Test
    fun `factory creates 1_0 and 1_1 interpreters`() {
        val interp10 = InterpreterFactory.create(LanguageVersion.V1_0)
        val interp11 = InterpreterFactory.create(LanguageVersion.V1_1)

        assertNotNull(interp10)
        assertNotNull(interp11)
    }

    @Test
    fun `1_0 interpreter rejects if statements`() {
        val code = "if (true) { println(1); }"
        val interpreter = InterpreterFactory.create10(BucketOutput())

        assertFailsWith<UnknownStatementError> {
            interpreter.interpret(parse(code))
        }
    }

    @Test
    fun `1_0 interpreter rejects boolean literals`() {
        val code = "println(true);"
        val interpreter = InterpreterFactory.create10(BucketOutput())

        assertFailsWith<UnknownExpressionError> {
            interpreter.interpret(parse(code))
        }
    }

    @Test
    fun `1_0 interpreter rejects readInput`() {
        val code = "println(readInput(\"prompt\"));"
        val interpreter = InterpreterFactory.create10(BucketOutput())

        assertFailsWith<UnknownExpressionError> {
            interpreter.interpret(parse(code))
        }
    }

    @Test
    fun `1_0 interpreter rejects readEnv`() {
        val code = "println(readEnv(\"MY_VAR\"));"
        val interpreter = InterpreterFactory.create10(BucketOutput())

        assertFailsWith<UnknownExpressionError> {
            interpreter.interpret(parse(code))
        }
    }

    @Test
    fun `const declaration and lookup works in 1_1`() {
        val code =
            """
            const x: number = 42;
            println(x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("42"), output.lines())
    }

    @Test
    fun `const reassignment throws CannotAssignToConstError`() {
        val code =
            """
            const PI: number = 3.14;
            PI = 3.14159;
            """.trimIndent()
        val interpreter = InterpreterFactory.create11()

        val error =
            assertFailsWith<CannotAssignToConstError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("PI", error.name)
    }

    @Test
    fun `readInput converts to number and can be used in arithmetic`() {
        val code =
            """
            let age: number = readInput("Enter age: ");
            let nextAge: number = age + 1;
            println(nextAge);
            """.trimIndent()
        val input = QueueInput("25")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        interpreter.interpret(parse(code))

        assertEquals(listOf("26"), output.lines())
    }

    @Test
    fun `readInput converts to boolean`() {
        val code =
            """
            let active: boolean = readInput("Is active: ");
            println(active);
            """.trimIndent()
        val input = QueueInput("true")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        interpreter.interpret(parse(code))

        assertEquals(listOf("true"), output.lines())
    }

    @Test
    fun `readInput invalid number conversion fails with ValueConversionError`() {
        val code = "let n: number = readInput(\"Enter: \");"
        val input = QueueInput("not_a_number")
        val interpreter = InterpreterFactory.create11(input = input)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(StringValue("not_a_number"), error.value)
        assertEquals("number", error.targetType)
    }

    @Test
    fun `readEnv reads environment variable and converts to number`() {
        val code =
            """
            let port: number = readEnv("PORT");
            println(port);
            """.trimIndent()
        val env = MapEnvProvider("PORT" to "8080")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, env = env)

        interpreter.interpret(parse(code))

        assertEquals(listOf("8080"), output.lines())
    }

    @Test
    fun `readEnv missing variable throws EnvVariableNotFoundError`() {
        val code = "let secret: string = readEnv(\"SECRET_KEY\");"
        val interpreter = InterpreterFactory.create11(env = MapEnvProvider())

        val error =
            assertFailsWith<EnvVariableNotFoundError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("SECRET_KEY", error.name)
    }

    @Test
    fun `readEnv invalid boolean conversion throws ValueConversionError`() {
        val code = "let debug: boolean = readEnv(\"DEBUG\");"
        val env = MapEnvProvider("DEBUG" to "maybe")
        val interpreter = InterpreterFactory.create11(env = env)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(StringValue("maybe"), error.value)
        assertEquals("boolean", error.targetType)
    }

    @Test
    fun `if statement executes thenBranch`() {
        val code =
            """
            let x: number = 10;
            if (true) {
                let y: number = 20;
                x = x + y;
                println("inside: " + x);
            }
            println("outside: " + x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("inside: 30", "outside: 30"), output.lines())
    }

    @Test
    fun `a variable declared inside an if block is not visible outside`() {
        val code =
            """
            if (true) {
                let y: number = 20;
            }
            println(y);
            """.trimIndent()
        val interpreter = InterpreterFactory.create11()

        assertFailsWith<UndeclaredVariableError> {
            interpreter.interpret(parse(code))
        }
    }

    @Test
    fun `if statement executes elseBranch when false`() {
        val code =
            """
            if (false) {
                println("then");
            } else {
                println("else");
            }
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("else"), output.lines())
    }

    @Test
    fun `variable shadowing inside if block does not overwrite outer variable`() {
        val code =
            """
            let a: string = "outer";
            if (true) {
                let a: string = "inner";
                println(a);
            }
            println(a);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("inner", "outer"), output.lines())
    }

    @Test
    fun `assignment with type conversion from readInput works`() {
        val code =
            """
            let x: number = 10;
            x = readInput("Enter: ");
            println(x);
            """.trimIndent()
        val input = QueueInput("50")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        interpreter.interpret(parse(code))

        assertEquals(listOf("50"), output.lines())
    }

    @Test
    fun `assignment with invalid conversion throws ValueConversionError`() {
        val code =
            """
            let x: number = 10;
            x = readInput("Enter: ");
            """.trimIndent()
        val input = QueueInput("not_a_number")
        val interpreter = InterpreterFactory.create11(input = input)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(StringValue("not_a_number"), error.value)
        assertEquals("number", error.targetType)
    }

    @Test
    fun `declaration without initializer allows subsequent assignment`() {
        val code =
            """
            let x: number;
            x = 42;
            println(x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("42"), output.lines())
    }

    @Test
    fun `accessing uninitialized variable throws UninitializedVariableError`() {
        val code =
            """
            let x: number;
            println(x);
            """.trimIndent()
        val interpreter = InterpreterFactory.create11()

        val error =
            assertFailsWith<UninitializedVariableError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("x", error.name)
    }

    // la posicion es la del identificador que no existe, no la del println que lo contiene
    @Test
    fun `an undeclared variable error points at the identifier`() {
        val code =
            """
            let a: number = 1;
            println(x);
            """.trimIndent()
        val interpreter = InterpreterFactory.create11()

        val error =
            assertFailsWith<UndeclaredVariableError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(Position(2, 9), error.start)
        assertEquals(error.start, error.end)
    }
}

// parsea con 1.1 para que los tests de 1.0 reciban nodos que su interprete no soporta
private fun parse(code: String): Iterator<Statement> {
    val version = LanguageVersion.V1_1
    val tokens = Lexer(CharStream(StringReader(code)), LexerFactory.defaultReaders(version)).tokenize()
    return Parser(tokens, version)
        .parse()
        .asSequence()
        .map { result ->
            when (result) {
                is ParseResult.Success -> result.statement
                is ParseResult.Failure -> error("el codigo del test no parsea: ${result.message} en ${result.start}")
            }
        }.iterator()
}
