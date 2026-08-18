package printscript.cli

import printscript.ast.Statement
import printscript.interpreter.Interpreter
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerInterface
import printscript.lexer.LexicalError
import printscript.parser.Parser
import printscript.parser.ParserInterface
import printscript.parser.SyntaxError
import printscript.parser.result.ParseResult
import java.io.StringReader

private val CODIGO = """
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

    try {
        runPrintScript(CODIGO , { linea -> println(linea) }) //aca los outputs van a la consola
    } catch (e: LexicalError) {
        println("Error lexico: ${e.message} (linea ${e.start.line})")
    } catch (e: SyntaxError) {
        println("Error de sintaxis: ${e.message} (linea ${e.start.line})")
    }
}

//arma la pipeline texto -> lexer -> parser -> interpreter
//output es a donde van los println del programa
fun runPrintScript(code: String, output: (String) -> Unit) {
    val lexer: LexerInterface = Lexer(CharStream(StringReader(code)))
    val parser: ParserInterface = Parser(lexer.tokenize())

    //cada statement se lexea, parsea y ejecuta recien cdo interpret() pide el siguiente
    val statements: Iterator<Statement> = parser.parse()
        .asSequence()
        .map { result ->
            when (result) {
                is ParseResult.Success -> result.statement
                is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
            }
        }
        .iterator()

    Interpreter(output).interpret(statements)
}
