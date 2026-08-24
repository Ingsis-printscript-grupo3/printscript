package printscript.cli

import printscript.ast.Statement
import printscript.interpreter.Interpreter
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerInterface
import printscript.lexer.LexicalError
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import java.io.StringReader

private val CODIGO =
    """
    let x: number = 5;
    let y: number = x * 3;
    println(y);
    let saludo: string = "hola";
    println(saludo);
    """.trimIndent()

fun main() {
    println("codigo:")
    println(CODIGO)
    println("output:")

    val lexer: LexerInterface = Lexer(CharStream(StringReader(CODIGO)))
    val parser = Parser(lexer.tokenize())

    val statements = mutableListOf<Statement>()

    try {
        for (result in parser.parse()) {
            when (result) {
                is ParseResult.Success -> statements.add(result.statement)
                is ParseResult.Failure -> {
                    println("Error de sintaxis: ${result.message}")
                    return
                }
            }
        }
    } catch (e: LexicalError) {
        println("Error lexico: ${e.message} (linea ${e.start.line})")
        return
    }

    Interpreter().interpret(statements.iterator())
}
