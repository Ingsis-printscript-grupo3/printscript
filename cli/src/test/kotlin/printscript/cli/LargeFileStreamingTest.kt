package printscript.cli

import org.junit.jupiter.api.Tag
import printscript.formatter.Formatter
import printscript.formatter.FormatterRules
import printscript.interpreter.output.Output
import printscript.runner.Engine
import printscript.runner.ExecutionResult
import printscript.runner.FormatResult
import java.io.File
import java.io.Writer
import kotlin.test.Test
import kotlin.test.assertTrue

private const val STATEMENTS = 32_768

// corre aparte del build normal, con poca memoria
@Tag("load")
class LargeFileStreamingTest {
    // el codigo se escribe en un archivo para no tenerlo entero en memoria
    private fun largeSource(): File =
        File.createTempFile("printscript-large", ".prs").apply {
            deleteOnExit()
            bufferedWriter().use { out ->
                repeat(STATEMENTS) { out.write("println(1);\n") }
            }
        }

    private fun discardingWriter(): Writer =
        object : Writer() {
            override fun write(
                cbuf: CharArray,
                off: Int,
                len: Int,
            ) = Unit

            override fun flush() = Unit

            override fun close() = Unit
        }

    private object DiscardingOutput : Output {
        override fun emit(line: String) = Unit
    }

    @Test
    fun `validates 32768 statements without running out of memory`() {
        val file = largeSource()

        val result = Engine(DiscardingOutput).validate(file.reader())

        assertTrue(result is ExecutionResult.Success)
    }

    @Test
    fun `formats 32768 statements without running out of memory`() {
        val file = largeSource()

        val result =
            Engine(DiscardingOutput).format({ file.reader() }) { tokens ->
                Formatter(FormatterRules()).format(tokens, discardingWriter())
            }

        assertTrue(result is FormatResult.Success)
    }
}
