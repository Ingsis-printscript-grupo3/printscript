package printscript.formatter

import printscript.lexer.CharStream
import printscript.lexer.Lexer
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatterTest {
    private fun tokensOf(code: String) = Lexer(CharStream(StringReader(code))).tokenize()

    private fun format(
        code: String,
        rules: FormatterRules = FormatterRules(),
    ): String {
        val writer = StringWriter()
        Formatter(rules).format(tokensOf(code), writer)
        return writer.toString()
    }

    @Test
    fun `without rules the code keeps its original spacing`() {
        val code = "let   x :number=5;\nif (x) {\n  println( x );\n}"

        assertEquals(code, format(code))
    }

    @Test
    fun `does not add a line break after the last statement`() {
        val rules = FormatterRules(lineBreaksAfterPrintln = 2)

        assertEquals("println(1);", format("println(1);", rules))
    }

    @Test
    fun `puts the brace of the else below too`() {
        val code = "if (x) {\n  println(1);\n} else {\n  println(2);\n}"
        val rules = FormatterRules(ifBraceBelowLine = true)

        assertEquals("if (x)\n{\n  println(1);\n} else\n{\n  println(2);\n}", format(code, rules))
    }

    @Test
    fun `indents the block but leaves the closing brace where it was`() {
        val code = "if (x) {\nif (y) {\nprintln(1);\n  }\n}"
        val rules = FormatterRules(indentInsideIf = 2)

        assertEquals("if (x) {\n  if (y) {\n    println(1);\n  }\n}", format(code, rules))
    }

    @Test
    fun `writes each token as soon as it reads it`() {
        val source = tokensOf("println(1);\n".repeat(1000))
        var read = 0
        var readAtFirstWrite = -1
        val counting = source.asSequence().onEach { read++ }.iterator()
        val spy =
            object : Writer() {
                override fun write(
                    cbuf: CharArray,
                    off: Int,
                    len: Int,
                ) {
                    if (readAtFirstWrite < 0) readAtFirstWrite = read
                }

                override fun flush() = Unit

                override fun close() = Unit
            }

        Formatter(FormatterRules()).format(counting, spy)

        // si juntara los tokens antes de escribir, al primer write ya los habria leido todos
        assertEquals(1, readAtFirstWrite)
    }
}
