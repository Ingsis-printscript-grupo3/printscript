package printscript.parser

import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.expression.InfixParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.statement.DefaultStatementHandlers
import printscript.parser.statement.StatementHandler

// recibe Iterator<Token> y no un Lexer: :parser no depende de :lexer y la fabrica no
// es excusa para acoplarlos
object ParserFactory {
    fun create(
        tokens: Iterator<Token>,
        version: LanguageVersion,
        prefixParselets: Map<TokenType, PrefixParselet> = defaultPrefixParselets(version),
        infixParselets: Map<TokenType, InfixParselet> = defaultInfixParselets(),
        statementHandlers: Map<TokenType, StatementHandler> = defaultStatementHandlers(version),
    ): ParserInterface =
        Parser(
            tokens = tokens,
            version = version,
            prefixParselets = prefixParselets,
            infixParselets = infixParselets,
            statementHandlers = statementHandlers,
        )

    fun create(
        tokens: Iterator<Token>,
        version: String,
    ): ParserInterface = create(tokens, LanguageVersion.parse(version))

    fun create10(tokens: Iterator<Token>): ParserInterface = create(tokens, LanguageVersion.V1_0)

    fun create11(tokens: Iterator<Token>): ParserInterface = create(tokens, LanguageVersion.V1_1)

    fun defaultPrefixParselets(version: LanguageVersion): Map<TokenType, PrefixParselet> =
        DefaultExpressionParselets.prefix(version)

    // los binarios son los mismos en las dos versiones
    fun defaultInfixParselets(): Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix

    fun defaultStatementHandlers(version: LanguageVersion): Map<TokenType, StatementHandler> =
        DefaultStatementHandlers.map(version)
}
