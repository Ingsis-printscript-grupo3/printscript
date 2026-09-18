package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.formatter.Formatter
import printscript.formatter.FormatterRules
import printscript.formatter.FormatterRulesLoader
import printscript.interpreter.output.ConsoleOutput
import printscript.linter.Linter
import printscript.linter.LinterRules
import printscript.linter.LinterRulesLoader
import printscript.runner.Engine
import printscript.runner.ExecutionResult
import printscript.runner.FormatResult
import printscript.runner.LintResult
import java.io.Writer

private val SUPPORTED_VERSIONS = LanguageVersion.entries.joinToString(", ") { it.label }
private const val DEFAULT_VERSION = "1.0"

// sin --config se aplican las reglas que la consigna pide siempre
private val DEFAULT_FORMATTER_RULES =
    FormatterRules(
        lineBreakAfterStatement = true,
        singleSpaceSeparation = true,
        spaceSurroundingOperations = true,
        ifBraceSameLine = true,
        indentInsideIf = 4,
    )

class PrintScriptCli : CliktCommand(name = "printscript") {
    override fun run() = Unit
}

class ExecuteCommand : CliktCommand(name = "execute", help = "Run a .prs file") {
    private val file by argument(help = "Path to the .prs file to execute")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val version by versionOption()
    private val quiet by quietOption()

    override fun run() {
        val languageVersion = requireSupportedVersion(version)
        val progress = ParsingProgress(showProgress(quiet))
        val engine = Engine(ConsoleOutput(), onProgress = progress::report)
        val result = engine.execute(file.reader(), languageVersion)
        progress.finish()
        when (result) {
            is ExecutionResult.Success -> Unit
            is ExecutionResult.Failure -> fail(result.type, result.message, result.start, result.end)
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
    private val quiet by quietOption()

    override fun run() {
        val languageVersion = requireSupportedVersion(version)
        val progress = ParsingProgress(showProgress(quiet))
        val engine = Engine(ConsoleOutput(), onProgress = progress::report)
        val result = engine.validate(file.reader(), languageVersion)
        progress.finish()
        when (result) {
            is ExecutionResult.Success -> echo("${file.path}: no errors found")
            is ExecutionResult.Failure -> fail(result.type, result.message, result.start, result.end)
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
    private val quiet by quietOption()

    override fun run() {
        val languageVersion = requireSupportedVersion(version)
        val rules = config?.let { LinterRulesLoader.fromFile(it.path) } ?: LinterRules()
        val progress = ParsingProgress(showProgress(quiet))
        val engine = Engine(ConsoleOutput(), onProgress = progress::report)

        var warningCount = 0
        val result =
            engine.lint(file.reader(), languageVersion) { statements ->
                Linter(rules).analyze(statements) { warning ->
                    warningCount++
                    echo("Warning at [${warning.position.line}:${warning.position.column}]: ${warning.message}")
                }
            }
        progress.finish()

        when (result) {
            is LintResult.Success -> if (warningCount == 0) echo("${file.path}: no warnings found")
            is LintResult.Failure -> fail(result.type, result.message, result.start, result.end)
        }
    }
}

class FormatCommand : CliktCommand(name = "format", help = "Format a .prs file and print the result") {
    private val file by argument(help = "Path to the .prs file to format")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val config by option("--config", help = "Path to a JSON or YAML file with formatting rules")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val version by versionOption()
    private val quiet by quietOption()

    override fun run() {
        val languageVersion = requireSupportedVersion(version)
        val rules = config?.let { FormatterRulesLoader.fromFile(it.path) } ?: DEFAULT_FORMATTER_RULES
        val progress = ParsingProgress(showProgress(quiet))
        val engine = Engine(ConsoleOutput(), onProgress = progress::report)

        val result =
            engine.format({ file.reader() }, languageVersion) { tokens ->
                Formatter(rules).format(tokens, echoWriter())
            }
        progress.finish()

        when (result) {
            is FormatResult.Success -> Unit
            is FormatResult.Failure -> fail(result.type, result.message, result.start, result.end)
        }
    }

    // asi no junta todo el texto antes de imprimirlo
    private fun echoWriter(): Writer =
        object : Writer() {
            override fun write(
                cbuf: CharArray,
                off: Int,
                len: Int,
            ) = echo(String(cbuf, off, len), trailingNewline = false)

            override fun flush() = Unit

            override fun close() = Unit
        }
}

// Reports progress to stderr as statements are parsed, so it never mixes with a command's own stdout output.
internal class ParsingProgress(private val enabled: Boolean) {
    private var shown = false

    fun report(parsedStatements: Int) {
        if (!enabled) return
        shown = true
        System.err.print("\rParsing... $parsedStatements statement(s) parsed")
    }

    fun finish() {
        if (shown) System.err.println()
    }
}

private fun CliktCommand.quietOption() = option("--quiet", help = "Do not print parsing progress").flag()

// el progreso pisa la misma linea, eso solo se ve bien en una terminal
private fun showProgress(quiet: Boolean): Boolean = !quiet && System.console() != null

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
    start: Position? = null,
    end: Position? = null,
): Nothing {
    echo(formatError(type, message, start, end), err = true)
    throw ProgramResult(1)
}

// el unico lugar del cli que arma el texto de un error
private fun formatError(
    type: String,
    message: String,
    start: Position?,
    end: Position?,
): String =
    if (start == null || end == null) {
        "Error $type: $message"
    } else {
        "[${start.line}:${start.column}-${end.line}:${end.column}] $type: $message"
    }

fun main(args: Array<String>) =
    PrintScriptCli()
        .subcommands(ValidateCommand(), ExecuteCommand(), FormatCommand(), AnalyzeCommand())
        .main(args)
