package printscript.cli

import printscript.formatter.FormatterRulesLoader
import printscript.formatter.PrintScriptFormatter
import printscript.interpreter.Interpreter
import printscript.interpreter.output.ConsoleOutput
import printscript.interpreter.output.Output
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexicalError
import printscript.linter.LinterRulesLoader
import printscript.linter.StaticCodeAnalyzer
import printscript.parser.Parser
import printscript.parser.SyntaxError
import printscript.parser.result.ParseResult
import printscript.semantic.SemanticAnalyzer
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
            "Execution" -> runPrintScript(code, ConsoleOutput())
            "Formatting" -> formatPrintScript(code, configFile)
            "Analyzing" -> analyzePrintScript(code, configFile)
            else -> println("Unknown operation: $operation")
        }
    } catch (e: LexicalError) {
        println("Error lexico: ${e.message} (linea ${e.start.line})")
    } catch (e: SyntaxError) {
        println("Error de sintaxis: ${e.message} (linea ${e.start.line})")
    } catch (e: Exception) {
        println(e.message)
    }
}

// arma la pipeline texto -> lexer -> parser -> interpreter
fun runPrintScript(
    code: String,
    output: Output,
) {
    val statementList = parseToAST(code)

    val semanticResults = SemanticAnalyzer().analyze(statementList)
    for (result in semanticResults) {
        if (result is SemanticResult.Failure) {
            throw Exception(result.message) // Throws semantic error
        }
    }

    Interpreter(output).interpret(statementList.iterator())
}

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
    val semanticResults = SemanticAnalyzer().analyze(statementList)
    for (result in semanticResults) {
        if (result is SemanticResult.Failure) {
            throw Exception(result.message)
        }
    }
    println("Validation successful.")
}

fun formatPrintScript(
    code: String,
    configFile: String?,
) {
    val statementList = parseToAST(code)
    val rules =
        if (configFile != null) {
            val configText = File(configFile).readText()
            if (configFile.endsWith(".yaml") || configFile.endsWith(".yml")) {
                FormatterRulesLoader.fromYaml(configText)
            } else {
                FormatterRulesLoader.fromJson(configText)
            }
        } else {
            printscript.formatter.FormatterRules()
        }

    val formattedCode = PrintScriptFormatter(rules).format(statementList)
    println(formattedCode)
}

fun analyzePrintScript(
    code: String,
    configFile: String?,
) {
    val statementList = parseToAST(code)
    val rules =
        if (configFile != null) {
            val configText = File(configFile).readText()
            if (configFile.endsWith(".yaml") || configFile.endsWith(".yml")) {
                LinterRulesLoader.fromYaml(configText)
            } else {
                LinterRulesLoader.fromJson(configText)
            }
        } else {
            printscript.linter.LinterRules()
        }

    val warnings = StaticCodeAnalyzer(rules).analyze(statementList.iterator())
    if (warnings.isEmpty()) {
        println("No linting warnings found.")
    } else {
        warnings.forEach { w ->
            println("Warning at [${w.position.line}:${w.position.column}]: ${w.message}")
        }
    }
}
