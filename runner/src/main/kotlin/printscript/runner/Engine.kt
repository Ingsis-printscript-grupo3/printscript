package printscript.runner

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.interpreter.InterpreterFactory
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.env.SystemEnvProvider
import printscript.interpreter.input.ConsoleInput
import printscript.interpreter.input.InputProvider
import printscript.interpreter.output.Output
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.parser.Parser
import printscript.parser.SyntaxError
import printscript.parser.result.ParseResult
import printscript.semantic.SemanticAnalyzer
import printscript.semantic.SemanticError
import printscript.semantic.SemanticResult
import java.io.Reader
import java.io.StringReader

sealed interface ExecutionResult {
    object Success : ExecutionResult

    data class Failure(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    ) : ExecutionResult
}

sealed interface FormatResult {
    object Success : FormatResult

    data class Failure(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    ) : FormatResult
}

sealed interface LintResult {
    object Success : LintResult

    data class Failure(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    ) : LintResult
}

class Engine(
    private val output: Output,
    private val input: InputProvider = ConsoleInput(),
    private val env: EnvProvider = SystemEnvProvider(),
    // por donde el Engine avisa cuantos statements lleva parseados, igual que output es por donde imprime
    private val onProgress: (Int) -> Unit = {},
) {
    fun execute(
        code: String,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
    ): ExecutionResult = execute(StringReader(code), languageVersion)

    fun execute(
        reader: Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
    ): ExecutionResult =
        runCatchingErrors {
            val interpreter = InterpreterFactory.create(languageVersion, output, input, env)
            interpreter.interpret(analyzedStatements(reader, languageVersion, onProgress))
        }.asExecutionResult()

    fun validate(
        code: String,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
    ): ExecutionResult = validate(StringReader(code), languageVersion)

    // validar es correr el pipeline entero sin interpretar: alcanza con recorrer los statements
    fun validate(
        reader: Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
    ): ExecutionResult =
        runCatchingErrors {
            consume(analyzedStatements(reader, languageVersion, onProgress))
        }.asExecutionResult()

    /**
     * Lee el fuente dos veces: primero para avisar si no parsea, despues para formatear los tokens.
     * Por eso recibe [openReader] y no un Reader ya abierto.
     */
    fun format(
        openReader: () -> Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        format: (Iterator<Token>) -> Unit,
    ): FormatResult =
        runCatchingErrors {
            openReader().use { consume(parseIntoAst(parserFor(it, languageVersion), onProgress)) }
            openReader().use { format(Lexer(CharStream(it)).tokenize()) }
        }.asFormatResult()

    // el linter trabaja sobre el ast crudo: no necesita el chequeo semantico
    fun lint(
        reader: Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        lint: (Iterator<Statement>) -> Unit,
    ): LintResult =
        runCatchingErrors {
            lint(parseIntoAst(parserFor(reader, languageVersion), onProgress))
        }.asLintResult()
}

// primer paso: lexer y parser encadenados sobre el mismo reader
private fun parserFor(
    reader: Reader,
    languageVersion: LanguageVersion,
): Parser = Parser(Lexer(CharStream(reader)).tokenize(), languageVersion)

// segundo paso: de resultados del parser a statements, cortando con SyntaxError en el primer fallo
private fun parseIntoAst(
    parser: Parser,
    onProgress: (Int) -> Unit,
): Iterator<Statement> {
    var parsedCount = 0
    return iterator {
        for (result in parser.parse()) {
            when (result) {
                is ParseResult.Success -> {
                    yield(result.statement)
                    onProgress(++parsedCount)
                }
                is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
            }
        }
    }
}

// tercer paso: de resultados del analizador a statements, cortando con SemanticError en el primer fallo
private fun buildValidStatementIterator(
    semanticResultIterator: Iterator<SemanticResult<Statement>>,
): Iterator<Statement> =
    iterator {
        for (result in semanticResultIterator) {
            when (result) {
                is SemanticResult.Success -> yield(result.value)
                is SemanticResult.Failure -> throw SemanticError(result.message, result.position)
            }
        }
    }

// chequeo semantico sobre el ast ya parseado
private fun analyzeAst(
    astIterator: Iterator<Statement>,
    languageVersion: LanguageVersion,
): Iterator<Statement> {
    val semanticAnalyzer = SemanticAnalyzer(languageVersion)
    return buildValidStatementIterator(semanticAnalyzer.analyze(astIterator))
}

// el pipeline completo: lexer -> parser -> semantico. Lo que sale de aca ya se puede interpretar
private fun analyzedStatements(
    reader: Reader,
    languageVersion: LanguageVersion,
    onProgress: (Int) -> Unit,
): Iterator<Statement> = analyzeAst(parseIntoAst(parserFor(reader, languageVersion), onProgress), languageVersion)

// el pipeline es perezoso: hasta que alguien no recorre el iterador, no se lexea ni se parsea nada
private fun consume(statements: Iterator<Statement>) {
    while (statements.hasNext()) statements.next()
}
