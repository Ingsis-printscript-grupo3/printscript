package printscript.runner

import printscript.common.LanguageVersion
import printscript.formatter.FormatterFactory
import printscript.formatter.FormatterRulesLoader
import printscript.interpreter.output.BucketOutput
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals

// el caso if-indent-inside-2 del TCK, entrando por Engine.format como entra el TCK.
// Nuestro TckGoldenTest llama al Formatter directo, y por eso no vio este fallo.
class TckFormatPathTest {
    private val config = "{\n  \"indent-inside-if\": 4\n}"

    private val source =
        listOf(
            "let something: boolean = true;",
            "if (something) {",
            "  if (something) {",
            "    println(\"Entered two ifs\");",
            "  }",
            "}",
        )

    private val golden =
        listOf(
            "let something: boolean = true;",
            "if (something) {",
            "    if (something) {",
            "        println(\"Entered two ifs\");",
            "    }",
            "}",
        )

    private fun format(
        source: String,
        config: String,
    ): String {
        val writer = StringWriter()
        val formatter = FormatterFactory.create(FormatterRulesLoader.fromJson(config))
        val result =
            Engine(BucketOutput()).format({ StringReader(source) }, LanguageVersion.V1_1) {
                formatter.format(it, writer)
            }
        assertEquals(FormatResult.Success, result)
        return writer.toString()
    }

    @Test
    fun `nested ifs are indented, with unix line endings`() {
        val formatted = format(source.joinToString("\n"), config)

        assertEquals(golden.joinToString("\n"), formatted)
    }

    // el TCK clonado en Windows tiene los .ps con CRLF
    @Test
    fun `nested ifs are indented, with windows line endings`() {
        val formatted = format(source.joinToString("\r\n"), config.replace("\n", "\r\n"))

        assertEquals(golden.joinToString("\n"), formatted.replace("\r\n", "\n"))
    }
}
