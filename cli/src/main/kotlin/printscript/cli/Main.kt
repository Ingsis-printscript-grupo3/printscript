package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import printscript.common.LanguageVersion
import printscript.formatter.Formatter
import printscript.formatter.FormatterRules
import printscript.formatter.FormatterRulesLoader
import printscript.interpreter.output.ConsoleOutput
import printscript.linter.Linter
import printscript.linter.LinterRules
import printscript.linter.LinterRulesLoader

private val SUPPORTED_VERSIONS = LanguageVersion.entries.joinToString(", ") { it.label }
private const val DEFAULT_VERSION = "1.0"

class PrintScriptCli : CliktCommand(name = "printscript") {
    override fun run() = Unit
}

class ExecuteCommand : CliktCommand(name = "execute", help = "Run a .prs file") {
    private val file by argument(help = "Path to the .prs file to execute")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val version by versionOption()

    override fun run() {
        val languageVersion = requireSupportedVersion(version)
        val engine = Engine(output = ConsoleOutput())
        val progress = ParsingProgress()
        val result = engine.execute(file.reader(), languageVersion, onProgress = progress::report)
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
        val languageVersion = requireSupportedVersion(version)
        val engine = Engine(output = ConsoleOutput())
        val progress = ParsingProgress()
        val result = engine.validate(file.reader(), languageVersion, onProgress = progress::report)
        progress.finish()
        when (result) {
            is ExecutionResult.Success -> echo("${file.path}: no errors found")
            is ExecutionResult.Failure -> fail(result.type, result.message)
        }
    }
}

class AnalyzeCommand : CliktCommand(
    name = "analyze",
    help = "Statically analyze a .prs file for style and best-practice violations",
) {
    private val file by argument(help = "Path to the .prs file to analyze")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val config by option("--config", help = "Path to a JSON or YAML file with linter rules")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val version by versionOption()

    override fun run() {
        val languageVersion = requireSupportedVersion(version)
        val rules = config?.let { LinterRulesLoader.fromFile(it.path) } ?: LinterRules()
        val engine = Engine(output = ConsoleOutput())
        val progress = ParsingProgress()

        val result =
            engine.lint(file.reader(), languageVersion, onProgress = progress::report) { statements ->
                Linter(rules).analyze(statements.iterator())
            }
        progress.finish()

        when (result) {
            is LintResult.Success -> {
                if (result.warnings.isEmpty()) {
                    echo("${file.path}: no warnings found")
                } else {
                    result.warnings.forEach { warning ->
                        echo("Warning at [${warning.position.line}:${warning.position.column}]: ${warning.message}")
                    }
                }
            }
            is LintResult.Failure -> fail(result.type, result.message)
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
        val languageVersion = requireSupportedVersion(version)
        val rules = config?.let { FormatterRulesLoader.fromFile(it.path) } ?: FormatterRules()
        val engine = Engine(output = ConsoleOutput())
        val progress = ParsingProgress()

        val result =
            engine.format(file.reader(), languageVersion, onProgress = progress::report) { statements ->
                Formatter(rules).format(statements)
            }
        progress.finish()

        when (result) {
            is FormatResult.Success -> echo(result.code, trailingNewline = false)
            is FormatResult.Failure -> fail(result.type, result.message)
        }
    }
}

// Reports progress to stderr as statements are parsed, so it never mixes with a command's own stdout output.
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
        help = "Version of the PrintScript language to use. Supported: $SUPPORTED_VERSIONS.",
    ).default(DEFAULT_VERSION)

private fun CliktCommand.requireSupportedVersion(version: String): LanguageVersion =
    runCatching { LanguageVersion.parse(version) }
        .getOrElse {
            fail(
                "UnsupportedVersion",
                "PrintScript version '$version' is not supported. Supported: $SUPPORTED_VERSIONS.",
            )
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
