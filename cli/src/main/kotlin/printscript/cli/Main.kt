package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.formatter.Formatter
import printscript.formatter.FormatterRules
import printscript.formatter.FormatterRulesLoader
import printscript.interpreter.output.ConsoleOutput
import printscript.linter.IdentifierFormat
import printscript.linter.LinterFactory
import printscript.linter.LinterInterface
import printscript.linter.LinterRules
import printscript.linter.LinterRulesLoader
import printscript.linter.Warning
import printscript.runner.Engine
import printscript.runner.ExecutionResult
import printscript.runner.FormatResult
import printscript.runner.LintResult
import java.io.Reader
import java.io.Writer

// usage: <command> <file> [--version=1.0|1.1] [--config=<path .json|.yaml|.yml>] [--quiet]
// commands: execute, validate, format, analyze (--config only for format and analyze)
// example: execute ejemplos\programas\ok\10-consigna-joe-doe.prs

private val DEFAULT_FORMATTER_RULES =
    FormatterRules(
        lineBreakAfterStatement = true,
        singleSpaceSeparation = true,
        spaceSurroundingOperations = true,
        ifBraceSameLine = true,
        indentInsideIf = 4,
    )

private val DEFAULT_LINTER_RULES =
    LinterRules(
        identifierFormat = IdentifierFormat.CAMEL_CASE,
        printCallArgumentsMustBeLiteralOrIdentifier = true,
        readInputArgumentsMustBeLiteralOrIdentifier = true,
    )

class PrintScriptCli : CliktCommand(name = "printscript") {
    override fun run() = Unit
}

// what the four commands share: the file, --version, --quiet, the progress and how an error is shown
abstract class PrintScriptCommand(name: String, help: String) : CliktCommand(name = name, help = help) {
    protected val file by argument(help = "Path to the .prs file")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    // only accepts the labels of LanguageVersion ("1.0", "1.1") and gives back the enum value
    private val version by option("--version", help = "Version of the PrintScript language to use")
        .choice(LanguageVersion.entries.associateBy(LanguageVersion::label))
        .default(LanguageVersion.DEFAULT)
    private val quiet by option("--quiet", help = "Do not print parsing progress").flag()

    protected val engine = Engine(output = ConsoleOutput())
    protected lateinit var progress: ParsingProgress

    protected abstract fun runOn(version: LanguageVersion)

    final override fun run() {
        // the progress rewrites the same line, which only looks right in a terminal
        progress = ParsingProgress(!quiet && System.console() != null)
        runOn(version)
    }

    // closes the progress and, if the engine returned an error, shows it and stops
    protected fun checkResult(result: ExecutionResult) {
        progress.finish()
        if (result is ExecutionResult.Failure) stop(result.type, result.message, result.start, result.end)
    }

    protected fun checkResult(result: FormatResult) {
        progress.finish()
        if (result is FormatResult.Failure) stop(result.type, result.message, result.start, result.end)
    }

    protected fun checkResult(result: LintResult) {
        progress.finish()
        if (result is LintResult.Failure) stop(result.type, result.message, result.start, result.end)
    }

    // an invalid config is reported like any other error, without the java stacktrace
    protected fun invalidConfig(error: IllegalArgumentException): Nothing =
        stop("Config", error.message ?: "Invalid config file")

    private fun stop(
        type: String,
        message: String,
        start: Position? = null,
        end: Position? = null,
    ): Nothing {
        echo(formatError(type, message, start, end), err = true)
        throw ProgramResult(1)
    }
}

// --------------------------------------------------------------------------------------------------------------------
// runs lexer, parser, semantic analysis and interpreter
class ExecuteCommand : PrintScriptCommand("execute", "Run a .prs file") {
    override fun runOn(version: LanguageVersion) {
        checkResult(engine.execute(file.reader(), version, progress::report))
    }
}

// runs lexer, parser and semantic analysis, but not interpreter
class ValidateCommand : PrintScriptCommand(
    "validate",
    "Check a .prs file for lexical, syntax and semantic errors without running it",
) {
    override fun runOn(version: LanguageVersion) {
        checkResult(engine.validate(file.reader(), version, progress::report))
        echo("${file.path}: no errors found")
    }
}

// runs lexer, parser and the linter (no semantic analysis)
class AnalyzeCommand : PrintScriptCommand(
    "analyze",
    "Statically analyze a .prs file for style and best-practice violations",
) {
    private val config by option("--config", help = "Path to a JSON or YAML file with linter rules")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private lateinit var linter: LinterInterface
    private var warningCount = 0

    override fun runOn(version: LanguageVersion) {
        linter = LinterFactory.create(version, loadRules())
        checkResult(engine.lint(file.reader(), version, progress::report, ::lintStatements))
        if (warningCount == 0) echo("${file.path}: no warnings found")
    }

    private fun loadRules(): LinterRules {
        val path = config?.path ?: return DEFAULT_LINTER_RULES
        return try {
            LinterRulesLoader.fromFile(path)
        } catch (e: IllegalArgumentException) {
            invalidConfig(e)
        }
    }

    private fun lintStatements(statements: Iterator<Statement>) = linter.analyze(statements, ::printWarning)

    private fun printWarning(warning: Warning) {
        warningCount++
        echo("Warning at [${warning.position.line}:${warning.position.column}]: ${warning.message}")
    }
}

// runs lexer and parser to validate, then formats the tokens (no semantic analysis)
class FormatCommand : PrintScriptCommand("format", "Format a .prs file and print the result") {
    private val config by option("--config", help = "Path to a JSON or YAML file with formatting rules")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private lateinit var rules: FormatterRules

    override fun runOn(version: LanguageVersion) {
        rules = loadRules()
        checkResult(engine.format(::openFile, version, progress::report, ::formatTokens))
    }

    private fun loadRules(): FormatterRules {
        val path = config?.path ?: return DEFAULT_FORMATTER_RULES
        return try {
            FormatterRulesLoader.fromFile(path)
        } catch (e: IllegalArgumentException) {
            invalidConfig(e)
        }
    }

    // the engine opens the file twice: once to validate it and once to format it
    private fun openFile(): Reader = file.reader()

    private fun formatTokens(tokens: Iterator<Token>) = Formatter(rules).format(tokens, echoWriter())

    // prints each piece as soon as it is formatted
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
// aux --------------------------------------------------------------------------------------------------------------

// prints the parsing progress to stderr, so it never mixes with the output of a command
class ParsingProgress(private val enabled: Boolean) {
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

// the only place in the cli that builds the text of an error
private fun formatError(
    type: String,
    message: String,
    start: Position?,
    end: Position?,
): String =
    when {
        start == null || end == null -> "Error $type: $message"
        // un error de un solo punto no repite la posicion dos veces
        start == end -> "[${start.line}:${start.column}] $type: $message"
        else -> "[${start.line}:${start.column}-${end.line}:${end.column}] $type: $message"
    }

fun main(args: Array<String>) =
    PrintScriptCli()
        .subcommands(ValidateCommand(), ExecuteCommand(), FormatCommand(), AnalyzeCommand())
        .main(args)
