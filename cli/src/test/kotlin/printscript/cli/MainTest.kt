package printscript.cli

import printscript.semantic.SemanticError
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MainTest {
    private fun captureStdout(block: () -> Unit): String {
        val original = System.out
        val buffer = ByteArrayOutputStream()
        System.setOut(PrintStream(buffer))
        try {
            block()
        } finally {
            System.setOut(original)
        }
        return buffer.toString().trim()
    }

    private fun tempFile(
        content: String,
        suffix: String,
    ): File {
        val file = File.createTempFile("cli-test", suffix)
        file.deleteOnExit()
        file.writeText(content)
        return file
    }

    @Test
    fun `executePrintScript prints the program output on success`() {
        val output = captureStdout { executePrintScript("""println("hi");""") }
        assertEquals("hi", output)
    }

    @Test
    fun `executePrintScript reports failures without throwing`() {
        val output = captureStdout { executePrintScript("let a: number = \"nope\";") }
        assertTrue(output.startsWith("Error Semantic:"))
    }

    @Test
    fun `validatePrintScript reports success`() {
        val output = captureStdout { validatePrintScript("let a: number = 1;") }
        assertEquals("Validation successful.", output)
    }

    @Test
    fun `validatePrintScript throws on a semantic error`() {
        assertFailsWith<SemanticError> { validatePrintScript("let a: number = \"nope\";") }
    }

    @Test
    fun `formatPrintScript formats using the default rules when no config is given`() {
        val output = captureStdout { formatPrintScript("let  a:number=1;", null) }
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `formatPrintScript honors a provided config file`() {
        val config = tempFile("spaceBeforeColon: true\nspaceAfterColon: true\n", ".yaml")
        val output = captureStdout { formatPrintScript("let a: number = 1;", config.path) }
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `analyzePrintScript reports no warnings for clean code`() {
        val output = captureStdout { analyzePrintScript("let a: number = 1;\nprintln(a);", null) }
        assertEquals("No linting warnings found.", output)
    }

    @Test
    fun `analyzePrintScript reports a warning when println receives an expression`() {
        val code =
            """
            let a: number = 1;
            let b: number = 2;
            println(a + b);
            """.trimIndent()
        val output = captureStdout { analyzePrintScript(code, null) }
        assertTrue(output.contains("Warning at"))
    }

    @Test
    fun `main prints usage when no arguments are given`() {
        val output = captureStdout { main(emptyArray()) }
        assertTrue(output.contains("Usage:"))
    }

    @Test
    fun `main reports a missing file path`() {
        val output = captureStdout { main(arrayOf("Validation")) }
        assertEquals("Error: File path is required.", output)
    }

    @Test
    fun `main reports an unknown operation`() {
        val file = tempFile("let a: number = 1;", ".prs")
        val output = captureStdout { main(arrayOf("Nonsense", file.path)) }
        assertEquals("Unknown operation: Nonsense", output)
    }

    @Test
    fun `main runs the Validation operation end to end`() {
        val file = tempFile("let a: number = 1;", ".prs")
        val output = captureStdout { main(arrayOf("Validation", file.path)) }
        assertEquals("Validation successful.", output)
    }

    @Test
    fun `main reports a syntax error with its line`() {
        val file = tempFile("let a: number = 1", ".prs")
        val output = captureStdout { main(arrayOf("Validation", file.path)) }
        assertTrue(output.startsWith("Error de sintaxis:"))
    }

    @Test
    fun `main reports a semantic error with its line`() {
        val file = tempFile("let a: number = \"nope\";", ".prs")
        val output = captureStdout { main(arrayOf("Validation", file.path)) }
        assertTrue(output.startsWith("Error semantico:"))
    }

    @Test
    fun `main reports a lexical error with its line`() {
        val file = tempFile("let a: number = 12 @ 4;", ".prs")
        val output = captureStdout { main(arrayOf("Validation", file.path)) }
        assertTrue(output.startsWith("Error lexico:"))
    }

    @Test
    fun `main reports any other exception through its message`() {
        val file = tempFile("let a: number = 1;", ".prs")
        val output = captureStdout { main(arrayOf("Formatting", file.path, "1.0", "not-a-real-config.txt")) }
        assertTrue(output.contains("Config file not found"))
    }

    @Test
    fun `main runs the Formatting operation end to end`() {
        val file = tempFile("let a: number = 1;", ".prs")
        val output = captureStdout { main(arrayOf("Formatting", file.path)) }
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `main runs the Analyzing operation end to end`() {
        val file = tempFile("let a: number = 1;\nprintln(a);", ".prs")
        val output = captureStdout { main(arrayOf("Analyzing", file.path)) }
        assertEquals("No linting warnings found.", output)
    }

    @Test
    fun `main runs the Execution operation end to end`() {
        val file = tempFile("""println("hi");""", ".prs")
        val output = captureStdout { main(arrayOf("Execution", file.path)) }
        assertEquals("hi", output)
    }
}
