package printscript.cli

import printscript.ast.Statement
import printscript.interpreter.Interpreter
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class E2ETest {

    private fun runScript(code: String): List<String> {
        val lexer = Lexer(CharStream(StringReader(code)))
        val parser = Parser(lexer.tokenize())
        val statements = mutableListOf<Statement>()

        for (result in parser.parse()) {
            when (result) {
                is ParseResult.Success -> statements.add(result.statement)
                is ParseResult.Failure -> throw Exception("Error de sintaxis: ${result.message}")
            }
        }

        val semanticResults = printscript.semantic.SemanticAnalyzer().analyze(statements)
        for (result in semanticResults) {
            if (result is printscript.semantic.SemanticResult.Failure) {
                throw Exception("Error semantico: ${result.message}")
            }
        }

        val output = mutableListOf<String>()
        val interpreter = Interpreter { text -> output.add(text) }
        interpreter.interpret(statements.iterator())
        return output
    }

    @Test
    fun `example 1 - string variable and output`() {
        val code = """
            let name: string = "Joe";
            let lastName: string = "Doe";
            println(name + " " + lastName);
        """.trimIndent()

        assertEquals(listOf("Joe Doe"), runScript(code))
    }

    @Test
    fun `example 2 - number division and string concatenation`() {
        val code = """
            let a: number = 12;
            let b: number = 4;
            let c: number = a / b;
            println("Result: " + c);
        """.trimIndent()

        assertEquals(listOf("Result: 3"), runScript(code))
    }

    @Test
    fun `example 3 - variable reassignment and math precedence`() {
        val code = """
            let a: number = 12;
            let b: number = 4;
            a = a / b;
            println("Result: " + a);
        """.trimIndent()

        assertEquals(listOf("Result: 3"), runScript(code))
    }

    @Test
    fun `error 1 - lexical error with invalid character`() {
        val code = """
            let a: number = 12 @ 4;
        """.trimIndent()

        // Lexer throws LexicalError which extends RuntimeException/Exception
        val exception = assertFailsWith<Exception> {
            runScript(code)
        }
        // Exception class might be LexicalError, so we just check it fails
    }

    @Test
    fun `error 2 - syntax error with missing semicolon`() {
        val code = """
            let a: number = 12
        """.trimIndent()

        val exception = assertFailsWith<Exception> {
            runScript(code)
        }
        assertTrue(exception.message!!.contains("Error de sintaxis"))
    }

    @Test
    fun `error 3 - semantic error with incompatible types`() {
        val code = """
            let a: number = "hola";
        """.trimIndent()

        val exception = assertFailsWith<Exception> {
            runScript(code)
        }
        assertTrue(exception.message!!.contains("Error semantico"))
    }
}
