package printscript.runner

import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrintScriptRunnerTest {
    private fun stream(text: String) = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun `execute 1_0 program prints output and reports no errors`() {
        val printed = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val code =
            """
            let a: number = 10;
            let b: number = 20;
            println(a + b);
            """.trimIndent()

        PrintScriptRunner.execute(
            src = stream(code),
            versionStr = "1.0",
            onPrint = printed::add,
            onInput = { "" },
            onError = errors::add,
        )

        assertEquals(listOf("30"), printed)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `execute 1_1 program with if and const works`() {
        val printed = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val code =
            """
            const condition: boolean = true;
            if (condition) {
                println("inside then");
            } else {
                println("inside else");
            }
            """.trimIndent()

        PrintScriptRunner.execute(
            src = stream(code),
            versionStr = "1.1",
            onPrint = printed::add,
            onInput = { "" },
            onError = errors::add,
        )

        assertEquals(listOf("inside then"), printed)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `execute 1_1 readInput emits prompt to onPrint before requesting input`() {
        val printed = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val code =
            """
            const name: string = readInput("Name:");
            println("Hello " + name + "!");
            """.trimIndent()

        PrintScriptRunner.execute(
            src = stream(code),
            versionStr = "1.1",
            onPrint = printed::add,
            onInput = { "world" },
            onError = errors::add,
        )

        assertEquals(listOf("Name:", "Hello world!"), printed)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `execute reports syntax error to onError without throwing`() {
        val errors = mutableListOf<String>()
        val code = "println(5)"

        PrintScriptRunner.execute(
            src = stream(code),
            versionStr = "1.0",
            onPrint = {},
            onInput = { "" },
            onError = errors::add,
        )

        assertEquals(1, errors.size)
        assertTrue(errors.first().contains("Expected"))
    }

    @Test
    fun `execute reports semantic error to onError without throwing`() {
        val errors = mutableListOf<String>()
        val code = "let a: number = \"hola\";"

        PrintScriptRunner.execute(
            src = stream(code),
            versionStr = "1.0",
            onPrint = {},
            onInput = { "" },
            onError = errors::add,
        )

        assertEquals(1, errors.size)
        assertTrue(errors.first().contains("Incompatible types"))
    }

    @Test
    fun `execute reports unknown version to onError`() {
        val errors = mutableListOf<String>()
        PrintScriptRunner.execute(
            src = stream("println(1);"),
            versionStr = "2.0",
            onPrint = {},
            onInput = { "" },
            onError = errors::add,
        )

        assertEquals(listOf("Unknown version: 2.0"), errors)
    }

    @Test
    fun `format formats code using json config stream`() {
        val code = "let a:string=\"test\";"
        val config =
            """
            {
              "enforce-spacing-around-equals": true,
              "enforce-spacing-after-colon-in-declaration": true
            }
            """.trimIndent()

        val writer = StringWriter()
        PrintScriptRunner.format(
            src = stream(code),
            versionStr = "1.0",
            config = stream(config),
            writer = writer,
        )

        assertEquals("let a: string = \"test\";", writer.toString().trim())
    }

    @Test
    fun `lint detects warnings from config stream`() {
        val code = "let a_snake_case: number = 10;"
        val config =
            """
            {
              "identifier_format": "camel case"
            }
            """.trimIndent()

        val errors = mutableListOf<String>()
        PrintScriptRunner.lint(
            src = stream(code),
            versionStr = "1.0",
            config = stream(config),
            onError = errors::add,
        )

        assertEquals(1, errors.size)
        assertTrue(errors.first().contains("camel case"))
    }

    @Test
    fun `lint reports syntax error to onError`() {
        val errors = mutableListOf<String>()
        PrintScriptRunner.lint(
            src = stream("println(5)"),
            versionStr = "1.0",
            config = stream("{}"),
            onError = errors::add,
        )

        assertEquals(1, errors.size)
        assertTrue(errors.first().contains("Expected"))
    }

    @Test
    fun `execute with empty prompt does not print prompt`() {
        val printed = mutableListOf<String>()
        val code = "let a: number = 5;\nprintln(a);"
        PrintScriptRunner.execute(
            src = stream(code),
            versionStr = "1.0",
            onPrint = printed::add,
            onInput = { "" },
            onError = {},
        )
        assertEquals(listOf("5"), printed)
    }

    @Test
    fun `format reports unknown version to onError`() {
        val errors = mutableListOf<String>()
        PrintScriptRunner.format(
            src = stream(""),
            versionStr = "3.0",
            config = stream("{}"),
            writer = StringWriter(),
            onError = errors::add,
        )
        assertEquals(listOf("Unknown version: 3.0"), errors)
    }

    @Test
    fun `lint reports unknown version to onError`() {
        val errors = mutableListOf<String>()
        PrintScriptRunner.lint(
            src = stream(""),
            versionStr = "3.0",
            config = stream("{}"),
            onError = errors::add,
        )
        assertEquals(listOf("Unknown version: 3.0"), errors)
    }

    @Test
    fun `execute reports lexical error to onError`() {
        val errors = mutableListOf<String>()
        PrintScriptRunner.execute(
            src = stream("let a: number = 12 @ 4;"),
            versionStr = "1.0",
            onPrint = {},
            onInput = { "" },
            onError = errors::add,
        )
        assertEquals(1, errors.size)
    }
}
