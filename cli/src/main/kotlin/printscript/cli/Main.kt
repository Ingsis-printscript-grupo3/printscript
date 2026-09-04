package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import printscript.formatter.FormatterRules
import printscript.formatter.FormatterRulesLoader
import printscript.formatter.PrintScriptFormatter
import printscript.interpreter.output.ConsoleOutput
import java.io.File

private const val SUPPORTED_VERSION = "1.0"

class PrintScriptCli : CliktCommand(name = "printscript") {
    override fun run() = Unit
}

class ExecuteCommand : CliktCommand(name = "execute", help = "Run a .prs file") {
    private val file by argument(help = "Path to the .prs file to execute")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val version by versionOption()

    override fun run() {
        requireSupportedVersion(version)
        val engine = Engine(output = ConsoleOutput())
        val progress = ParsingProgress()
        val result = engine.execute(file.reader(), onProgress = progress::report)
        progress.finish()
        when (result) {
            is ExecutionResult.Success -> Unit
            is ExecutionResult.Failure -> fail(result.type, result.message)
        }
    }
}

class ValidateCommand : CliktCommand(
    name = "validate",
    help = "Check a .prs file for lexical, syntax and semantic errors without running it",
) {
    private val file by argument(help = "Path to the .prs file to validate")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val version by versionOption()

    override fun run() {
        requireSupportedVersion(version)
        val engine = Engine(output = ConsoleOutput())
        val progress = ParsingProgress()
        val result = engine.validate(file.reader(), onProgress = progress::report)
        progress.finish()
        when (result) {
            is ExecutionResult.Success -> echo("${file.path}: no errors found")
            is ExecutionResult.Failure -> fail(result.type, result.message)
        }
    }
}

class AnalyzeCommand : CliktCommand(
    name = "analyze",
    help =
        "Statically analyze a .prs file. Currently runs the same lexical/syntax/semantic " +
            "checks as 'validate' — rule-based static analysis (naming conventions, etc.) is not implemented yet.",
) {
    private val file by argument(help = "Path to the .prs file to analyze")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val config by option("--config", help = "Path to a rules config file (reserved for future analysis rules)")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val version by versionOption()

    override fun run() {
        requireSupportedVersion(version)
        val engine = Engine(output = ConsoleOutput())
        val progress = ParsingProgress()
        val result = engine.validate(file.reader(), onProgress = progress::report)
        progress.finish()
        when (result) {
            is ExecutionResult.Success -> echo("${file.path}: no errors found")
            is ExecutionResult.Failure -> fail(result.type, result.message)
        }
    }
}

class FormatCommand : CliktCommand(name = "format", help = "Format a .prs file and print the result") {
    private val file by argument(help = "Path to the .prs file to format")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val config by option("--config", help = "Path to a JSON or YAML file with formatting rules")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val version by versionOption()

    override fun run() {
        requireSupportedVersion(version)
        val rules = config?.let(::loadRules) ?: FormatterRules()
        val engine = Engine(output = ConsoleOutput())
        val progress = ParsingProgress()

        val result =
            engine.format(file.reader(), onProgress = progress::report) { statements ->
                PrintScriptFormatter(rules).format(statements)
            }
        progress.finish()

        when (result) {
            is FormatResult.Success -> echo(result.code, trailingNewline = false)
            is FormatResult.Failure -> fail(result.type, result.message)
        }
    }

    private fun loadRules(configFile: File): FormatterRules {
        val isYaml = configFile.extension.equals("yaml", ignoreCase = true) || configFile.extension.equals("yml", ignoreCase = true)
        return if (isYaml) {
            FormatterRulesLoader.fromYaml(configFile.readText())
        } else {
            FormatterRulesLoader.fromJson(configFile.readText())
        }
    }
}

/** Reports parsing progress to stderr as statements are parsed, so it never mixes with a command's own stdout output. */
private class ParsingProgress {
    private var shown = false

    fun report(parsedStatements: Int) {
        shown = true
        System.err.print("\rParsing... $parsedStatements statement(s) parsed")
    }

    fun finish() {
        if (shown) System.err.println()
    }
}

private fun CliktCommand.versionOption() =
    option(
        "--version",
        help = "Version of the PrintScript language to use. Only \"$SUPPORTED_VERSION\" is supported for now.",
    ).default(SUPPORTED_VERSION)

private fun CliktCommand.requireSupportedVersion(version: String) {
    if (version != SUPPORTED_VERSION) {
        fail(
            "UnsupportedVersion",
            "PrintScript version '$version' is not supported. Only '$SUPPORTED_VERSION' is supported for now.",
        )
    }
}

private fun CliktCommand.fail(
    type: String,
    message: String,
): Nothing {
    echo("Error $type: $message", err = true)
    throw ProgramResult(1)
}

fun main(args: Array<String>) =
    PrintScriptCli()
        .subcommands(ValidateCommand(), ExecuteCommand(), FormatCommand(), AnalyzeCommand())
        .main(args)
