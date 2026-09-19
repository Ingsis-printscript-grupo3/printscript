package printscript.runner

import printscript.common.LanguageVersion
import printscript.formatter.Formatter
import printscript.formatter.FormatterRules
import printscript.interpreter.output.BucketOutput
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EndToEndTest {
    private fun runEngine(
        code: String,
        version: LanguageVersion = LanguageVersion.V1_1,
    ): Pair<ExecutionResult, List<String>> {
        val bucket = BucketOutput()
        val engine = Engine(output = bucket)
        val result = engine.execute(code, version)
        return Pair(result, bucket.lines())
    }

    @Test
    fun `declares a variable and prints it`() {
        val (result, output) =
            runEngine(
                """
                let greeting: string = "hello";
                println(greeting);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("hello"), output)
    }

    @Test
    fun `uses a declared variable inside an operation`() {
        val (result, output) =
            runEngine(
                """
                let x: number = 5;
                let y: number = x * 3;
                println(y);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("15"), output)
    }

    @Test
    fun `reassigns a variable and the print reflects the new value`() {
        val (result, output) =
            runEngine(
                """
                let counter: number = 1;
                println(counter);
                counter = counter + 9;
                println(counter);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("1", "10"), output)
    }

    @Test
    fun `consigna example - concatenation of two string variables Joe Doe`() {
        val (result, output) =
            runEngine(
                """
                let name: string = "Joe";
                let lastName: string = "Doe";
                println(name + " " + lastName);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("Joe Doe"), output)
    }

    @Test
    fun `consigna example - division stored in a variable and concatenated`() {
        val (result, output) =
            runEngine(
                """
                let a: number = 12;
                let b: number = 4;
                let c: number = a / b;
                println("Result: " + c);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `consigna example - reassignment of an already declared variable`() {
        val (result, output) =
            runEngine(
                """
                let a: number = 12;
                let b: number = 4;
                a = a / b;
                println("Result: " + a);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("Result: 3"), output)
    }

    @Test
    fun `error 1 - lexical error with invalid character`() {
        val code =
            """
            let a: number = 12 @ 4;
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Lexical", result.type)
    }

    @Test
    fun `error 2 - syntax error with missing semicolon`() {
        val code =
            """
            let a: number = 12
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
        assertTrue(result.message.contains("Expected"))
    }

    @Test
    fun `error 3 - semantic error with incompatible types`() {
        val code =
            """
            let a: number = "hello";
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
        assertTrue(result.message.contains("Incompatible types"))
    }

    @Test
    fun `error 4 - runtime error when using an uninitialized variable`() {
        val code =
            """
            let x: number;
            println(x);
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Runtime", result.type)
    }

    // hasta que el interprete no implemento ScriptError, un error de runtime llegaba sin posicion.
    // una variable sin declarar la caza el semantico, asi que para llegar al interprete hace falta
    // una que este declarada pero sin inicializar
    @Test
    fun `a runtime error reports the position of the identifier that failed`() {
        val code =
            """
            let x: number;
            println(x);
            """.trimIndent()

        val (result, _) = runEngine(code)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Runtime", result.type)
        assertEquals(2, result.start?.line)
        assertEquals(9, result.start?.column)
        assertEquals(result.start, result.end)
    }

    @Test
    fun `the semantic error reports the line of the failing statement`() {
        val code =
            """
            let a: number = 1;
            let b: string = 2;
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
        assertEquals(2, result.start?.line)
        // apunta al let, que es donde arranca el statement
        assertEquals(1, result.start?.column)
        // el semantico no produce un end propio: el AST guarda un solo punto por nodo
        assertEquals(result.start, result.end)
    }

    @Test
    fun `a lexical error carries the range of the offending character`() {
        val (result, _) = runEngine("let a: number = 12 @ 4;")

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Lexical", result.type)
        assertEquals(1, result.start?.line)
        assertEquals(20, result.start?.column)
        assertEquals(21, result.end?.column)
    }

    @Test
    fun `a syntax error carries a range, not just a line`() {
        val (result, _) = runEngine("println(5)")

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
        assertEquals(1, result.start?.line)
        assertEquals(1, result.end?.line)
        assertNotNull(result.start?.column)
        assertNotNull(result.end?.column)
    }

    @Test
    fun `the engine reports progress once per parsed statement`() {
        val reported = mutableListOf<Int>()
        val code = "let a: number = 1;\nlet b: number = 2;\nprintln(a + b);"

        Engine(BucketOutput()).validate(StringReader(code), LanguageVersion.V1_0, onProgress = reported::add)

        assertEquals(listOf(1, 2, 3), reported)
    }

    @Test
    fun `format formats successfully through engine`() {
        val engine = Engine(BucketOutput())
        var called = false
        val result =
            engine.format({ StringReader("let a:number=5;") }, LanguageVersion.V1_0) { tokens ->
                tokens.forEach { called = true }
            }
        assertTrue(result is FormatResult.Success)
        assertTrue(called)
    }

    @Test
    fun `format returns failure on syntax error`() {
        val engine = Engine(BucketOutput())
        val result =
            engine.format({ StringReader("let a:number =") }, LanguageVersion.V1_0) { tokens ->
                tokens.forEach { }
            }
        assertTrue(result is FormatResult.Failure)
        assertEquals("Syntax", result.type)
    }

    @Test
    fun `lint analyzes successfully through engine`() {
        val engine = Engine(BucketOutput())
        var called = false
        val result =
            engine.lint(StringReader("let a: number = 5;"), LanguageVersion.V1_0) { statements ->
                statements.forEach { called = true }
            }
        assertTrue(result is LintResult.Success)
        assertTrue(called)
    }

    @Test
    fun `lint returns failure on syntax error`() {
        val engine = Engine(BucketOutput())
        val result =
            engine.lint(StringReader("let a: number ="), LanguageVersion.V1_0) { statements ->
                statements.forEach { }
            }
        assertTrue(result is LintResult.Failure)
        assertEquals("Syntax", result.type)
    }

    private fun formatOnce(code: String): String {
        val writer = StringWriter()
        val rules = FormatterRules(spaceAfterColon = true, spacingAroundEquals = true, indentInsideIf = 2)
        Engine(BucketOutput()).format({ StringReader(code) }, LanguageVersion.V1_1) { tokens ->
            Formatter(rules).format(tokens, writer)
        }
        return writer.toString()
    }

    @Test
    fun `formatting twice gives the same result`() {
        val code = "let   x :number=5;\nif(true){println(x);}"

        val once = formatOnce(code)

        assertEquals(once, formatOnce(once))
    }

    @Test
    fun `executes negative numeric literals`() {
        val (result, output) =
            runEngine(
                """
                let x: number = -5;
                let y: number = -3.14;
                println(x);
                println(y);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("-5", "-3.14"), output)
    }

    @Test
    fun `executes unary minus on variable and parenthesized expression`() {
        val (result, output) =
            runEngine(
                """
                let a: number = 5;
                let b: number = -a;
                println(b);
                let c: number = -(2 + 3);
                println(c);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("-5", "-5"), output)
    }

    @Test
    fun `executes chained minus and subtraction of negative number`() {
        val (result, output) =
            runEngine(
                """
                println(5 - -3);
                println(- -5);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("8", "5"), output)
    }

    @Test
    fun `executes operator precedence and left associativity correctly`() {
        val (result, output) =
            runEngine(
                """
                println(1 + 2 * 3);
                println(10 - 4 - 2);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("7", "4"), output)
    }

    @Test
    fun `execution fails on trailing unary minus`() {
        val (result, _) =
            runEngine(
                """
                let x: number = -;
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
    }

    @Test
    fun `executes negative numbers and unary minus under PrintScript 1_0`() {
        val (result, output) =
            runEngine(
                """
                let a: number = -10;
                let b: number = -a;
                println(a);
                println(b);
                println(5 - -3);
                """.trimIndent(),
                LanguageVersion.V1_0,
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("-10", "10", "8"), output)
    }

    @Test
    fun `executes variable reassignment with negative numbers and expressions`() {
        val (result, output) =
            runEngine(
                """
                let x: number = 10;
                x = -5;
                println(x);
                x = -x;
                println(x);
                x = 5 - -3;
                println(x);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("-5", "5", "8"), output)
    }

    @Test
    fun `executes const declaration with negative numbers`() {
        val (result, output) =
            runEngine(
                """
                const x: number = -42;
                println(x);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("-42"), output)
    }

    @Test
    fun `executes arithmetic operations with negative numbers`() {
        val (result, output) =
            runEngine(
                """
                println(5 + -3);
                println(-5 * -3);
                println(-10 / 2);
                println(-10 / -2);
                println(- - -5);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("2", "15", "-5", "5", "-5"), output)
    }

    @Test
    fun `execution fails when unary minus is applied to a string`() {
        val (result, _) =
            runEngine(
                """
                let x: number = -"hello";
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
    }

    @Test
    fun `execution fails when unary minus is applied to a boolean`() {
        val (result, _) =
            runEngine(
                """
                let x: number = -true;
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
    }

    @Test
    fun `execution fails on assignment with trailing unary minus`() {
        val (result, _) =
            runEngine(
                """
                let x: number = 5;
                x = -;
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
    }

    @Test
    fun `execution fails when expression has unary minus followed by multiply`() {
        val (result, _) =
            runEngine(
                """
                let x: number = - * 5;
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
    }

    @Test
    fun `execution fails on trailing unary minus inside binary expression`() {
        val (result, _) =
            runEngine(
                """
                let x: number = 5 + -;
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
    }
}
