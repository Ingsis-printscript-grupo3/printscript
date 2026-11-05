package printscript.parser

import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.expression.InfixParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.statement.StatementHandler

// recibe Iterator<Token> y no un Lexer: :parser no depende de :lexer y la fabrica no
// es excusa para acoplarlos
object ParserFactory {
    fun create(
        tokens: Iterator<Token>,
        prefixParselets: Map<TokenType, PrefixParselet>,
        statementHandlers: Map<TokenType, StatementHandler>,
        infixParselets: Map<TokenType, InfixParselet> = defaultInfixParselets(),
    ): Parser =
        Parser(
            tokens = tokens,
            prefixParselets = prefixParselets,
            infixParselets = infixParselets,
            statementHandlers = statementHandlers,
        )

    // los binarios son los mismos en todas las versiones
    fun defaultInfixParselets(): Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix
}
