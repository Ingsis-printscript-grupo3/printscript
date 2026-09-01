package printscript.cli

import printscript.formatter.Formatter
import printscript.formatter.FormatterRules
import printscript.formatter.FormatterRulesLoader
import printscript.interpreter.output.ConsoleOutput
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexicalError
import printscript.linter.Linter
import printscript.linter.LinterRules
import printscript.linter.LinterRulesLoader
import printscript.parser.Parser
import printscript.parser.SyntaxError
import printscript.parser.result.ParseResult
import printscript.semantic.SemanticAnalyzer
import printscript.semantic.SemanticError
import printscript.semantic.SemanticResult
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: <Operation> <FilePath> [<Version>] [<ConfigFile>]")
        println("Operations: Validation, Execution, Formatting, Analyzing")
        return
    }

    val operation = args[0]
    val filePath = args.getOrNull(1) ?: return println("Error: File path is required.")
    val version = args.getOrNull(2) ?: "1.0"
    val configFile = args.getOrNull(3)

    val code = File(filePath).readText()

    try {
        when (operation) {
            "Validation" -> validatePrintScript(code)
            "Execution" -> executePrintScript(code)
            "Formatting" -> formatPrintScript(code, configFile)
            "Analyzing" -> analyzePrintScript(code, configFile)
            else -> println("Unknown operation: $operation")
        }
    } catch (e: LexicalError) {
        println("Error lexico: ${e.message} (linea ${e.start.line})")
    } catch (e: SyntaxError) {
        println("Error de sintaxis: ${e.message} (linea ${e.start.line})")
    } catch (e: SemanticError) {
        println("Error semantico: ${e.message} (linea ${e.start.line})")
    } catch (e: Exception) {
        println(e.message)
    }
}

fun executePrintScript(code: String) {
    val engine = Engine(output = ConsoleOutput())
    when (val result = engine.execute(code)) {
        is ExecutionResult.Success -> {
            // Execution finished successfully, outputs are handled by the callback
        }
        is ExecutionResult.Failure -> {
            println("Error ${result.type}: ${result.message}")
        }
    }
}

// corre lexer y parser y pasa lista de Statements
private fun parseToAST(code: String) =
    Parser(Lexer(CharStream(java.io.StringReader(code))).tokenize())
        .parse()
        .asSequence()
        .map { result ->
            when (result) {
                is ParseResult.Success -> result.statement
                is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
            }
        }
        .toList()

fun validatePrintScript(code: String) {
    val statementList = parseToAST(code)
    val semanticResults = SemanticAnalyzer().analyze(statementList.iterator())
    for (result in semanticResults) {
        if (result is SemanticResult.Failure) {
            throw SemanticError(result.message, result.position)
        }
    }
    println("Validation successful.")
}

fun formatPrintScript(
    code: String,
    configFile: String?,
) {
    val statementList = parseToAST(code)
    val rules = if (configFile != null) FormatterRulesLoader.fromFile(configFile) else FormatterRules()

    val formattedCode = Formatter(rules).format(statementList)
    println(formattedCode)
}

fun analyzePrintScript(
    code: String,
    configFile: String?,
) {
    val statementList = parseToAST(code)
    val rules = if (configFile != null) LinterRulesLoader.fromFile(configFile) else LinterRules()

    val warnings = Linter(rules).analyze(statementList.iterator())
    if (warnings.isEmpty()) {
        println("No linting warnings found.")
    } else {
        warnings.forEach { w ->
            println("Warning at [${w.position.line}:${w.position.column}]: ${w.message}")
        }
    }
}
