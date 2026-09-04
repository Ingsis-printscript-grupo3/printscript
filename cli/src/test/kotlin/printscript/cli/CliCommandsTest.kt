package printscript.cli

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliCommandsTest {
    private val tempFiles = mutableListOf<File>()

    private fun prsFile(code: String): File {
        val file = File.createTempFile("printscript-cli-test", ".prs")
        file.writeText(code)
        tempFiles += file
        return file
    }

    @AfterTest
    fun cleanup() {
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    @Test
    fun `execute runs a valid file and prints its output`() {
        val file = prsFile("let saludo: string = \"hola\";\nprintln(saludo);\n")

        // ConsoleOutput writes straight to System.out (not through Clikt's echo), so it has
        // to be captured separately from the CliktCommandTestResult's stdout.
        val originalOut = System.out
        val capturedOut = ByteArrayOutputStream()
        System.setOut(PrintStream(capturedOut))
        val result =
            try {
                ExecuteCommand().test(listOf(file.path))
            } finally {
                System.setOut(originalOut)
            }

        assertEquals(0, result.statusCode)
        assertTrue(capturedOut.toString().contains("hola"))
    }

    @Test
    fun `execute reports an error and exits non-zero for invalid code`() {
        val file = prsFile("let a: number = \"hola\";\n")

        val result = ExecuteCommand().test(listOf(file.path))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("Semantic"))
    }

    @Test
    fun `validate accepts a well-formed file without executing it`() {
        val file = prsFile("let saludo: string = \"hola\";\nprintln(saludo);\n")

        val result = ValidateCommand().test(listOf(file.path))

        assertEquals(0, result.statusCode)
        assertTrue(result.stdout.contains("no errors found"))
    }

    @Test
    fun `validate reports the error type for a broken file`() {
        val file = prsFile("let a: number = 12\n")

        val result = ValidateCommand().test(listOf(file.path))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("Syntax"))
    }

    @Test
    fun `analyze runs the same checks as validate`() {
        val file = prsFile("let saludo: string = \"hola\";\nprintln(saludo);\n")

        val result = AnalyzeCommand().test(listOf(file.path))

        assertEquals(0, result.statusCode)
        assertTrue(result.stdout.contains("no errors found"))
    }

    @Test
    fun `format prints the formatted code without touching the original file`() {
        val originalCode = "let   saludo :string=\"hola\";\nprintln(saludo);\n"
        val file = prsFile(originalCode)

        val result = FormatCommand().test(listOf(file.path))

        assertEquals(0, result.statusCode)
        assertEquals("let saludo: string = \"hola\";\nprintln(saludo);\n", result.stdout)
        assertEquals(originalCode, file.readText())
    }

    @Test
    fun `format applies rules loaded from a JSON --config file`() {
        val file = prsFile("let saludo: string = \"hola\";\n")
        val config = File.createTempFile("printscript-cli-test-rules", ".json")
        tempFiles += config
        config.writeText("""{"spaceBeforeColon": true, "spaceAfterColon": false}""")

        val result = FormatCommand().test(listOf(file.path, "--config", config.path))

        assertEquals(0, result.statusCode)
        assertEquals("let saludo :string = \"hola\";\n", result.stdout)
    }

    @Test
    fun `format reports an error and exits non-zero for invalid code`() {
        val file = prsFile("let a: number = \n")

        val result = FormatCommand().test(listOf(file.path))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.isNotBlank())
    }

    @Test
    fun `validate accepts the supported --version explicitly`() {
        val file = prsFile("let saludo: string = \"hola\";\nprintln(saludo);\n")

        val result = ValidateCommand().test(listOf(file.path, "--version", "1.0"))

        assertEquals(0, result.statusCode)
    }

    @Test
    fun `validate rejects an unsupported --version`() {
        val file = prsFile("let saludo: string = \"hola\";\nprintln(saludo);\n")

        val result = ValidateCommand().test(listOf(file.path, "--version", "2.0"))

        assertEquals(1, result.statusCode)
        assertTrue(result.stderr.contains("UnsupportedVersion"))
    }

    @Test
    fun `validate reports parsing progress to stderr`() {
        val file = prsFile("let a: number = 1;\nlet b: number = 2;\nprintln(a + b);\n")

        // The progress reporter writes straight to System.err (not through Clikt's echo),
        // same reason as ConsoleOutput above.
        val originalErr = System.err
        val capturedErr = ByteArrayOutputStream()
        System.setErr(PrintStream(capturedErr))
        val result =
            try {
                ValidateCommand().test(listOf(file.path))
            } finally {
                System.setErr(originalErr)
            }

        assertEquals(0, result.statusCode)
        val progressOutput = capturedErr.toString()
        assertTrue(progressOutput.contains("Parsing..."))
        assertTrue(progressOutput.contains("3 statement(s) parsed"))
    }

    @Test
    fun `each subcommand is registered under the root command`() {
        val result =
            PrintScriptCli().subcommands(ValidateCommand(), ExecuteCommand(), FormatCommand(), AnalyzeCommand())
                .test(listOf("--help"))

        assertEquals(0, result.statusCode)
        listOf("validate", "execute", "format", "analyze").forEach { name ->
            assertTrue(result.stdout.contains(name), "expected --help output to mention '$name'")
        }
    }
}
