package printscript.cli

import printscript.interpreter.Interpreter
import printscript.interpreter.output.ConsoleOutput
import printscript.interpreter.output.Output
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexerInterface
import printscript.lexer.LexicalError
import printscript.parser.Parser
import printscript.parser.ParserInterface
import printscript.parser.SyntaxError
import printscript.parser.result.ParseResult
import printscript.semantic.SemanticAnalyzer
import printscript.semantic.SemanticResult
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

    try {
        runPrintScript(CODIGO, ConsoleOutput()) // aca los outputs van a la consola
    } catch (e: LexicalError) {
        println("Error lexico: ${e.message} (linea ${e.start.line})")
    } catch (e: SyntaxError) {
        println("Error de sintaxis: ${e.message} (linea ${e.start.line})")
    } catch (e: Exception) {
        println(e.message) // Thrown by semantic analyzer
    }
}

// arma la pipeline texto -> lexer -> parser -> interpreter
fun runPrintScript(
    code: String,
    output: Output,
) {
    val lexer: LexerInterface = Lexer(CharStream(StringReader(code)))
    val parser: ParserInterface = Parser(lexer.tokenize())

    // cada statement se lexea y parsea aca
    val statementList =
        parser.parse()
            .asSequence()
            .map { result ->
                when (result) {
                    is ParseResult.Success -> result.statement
                    is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
                }
            }
            .toList()

    val semanticResults = SemanticAnalyzer().analyze(statementList)
    for (result in semanticResults) {
        if (result is SemanticResult.Failure) {
            throw Exception(result.message) // Throws semantic error
        }
    }

    Interpreter(output).interpret(statementList.iterator())
}
