package printscript.parser.statement.handlers

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionFeatures

object VariableDeclarationHandler : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement> {
        // el StatementParser consume el let o el const antes de despachar el handler.
        // en 1.0 el const no esta registrado, asi que aca ya no se mira la version
        val keywordToken = checkNotNull(stream.previous()) { "the declaration handler runs after let or const" }
        val isConst = keywordToken.type == TokenType.CONST

        val nameToken =
            when (val result = stream.consume(TokenType.IDENTIFIER, "Expected variable name.")) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        val colonResult = stream.consume(TokenType.COLON, "Expected ':'.")
        if (colonResult is ASTResult.Failure) return colonResult

        val typeToken =
            when (val result = parseType(stream, statementParser.version)) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        val initializer =
            when (val result = parseInitializer(stream, expressionParser, isConst, keywordToken)) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        val semiResult = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semiResult is ASTResult.Failure) return semiResult

        return ASTResult.Success(
            VariableDeclaration(
                nameToken.value,
                typeToken.value,
                initializer,
                keywordToken.start,
                isConst,
                nameToken.start,
            ),
        )
    }

    // el tipo no abre la sentencia, asi que no se resuelve por el mapa de handlers:
    // se acepta cualquier tipo y VersionFeatures dice si esta disponible en esta version
    private fun parseType(
        stream: TokenStream,
        version: LanguageVersion,
    ): ASTResult<Token> {
        if (!stream.match(TokenType.NUMBERTYPE, TokenType.STRINGTYPE, TokenType.BOOLEANTYPE)) {
            val errorToken = stream.peek()
            // siempre hay un token previo: el ':' que se acaba de consumir
            val pos = errorToken?.start ?: checkNotNull(stream.previous()) { "the type follows a ':'" }.end
            return ASTResult.Failure("Expected 'number', 'string' or 'boolean'.", pos, errorToken?.end ?: pos)
        }
        // match() devolvio true, asi que acaba de consumir el token del tipo
        val typeToken = checkNotNull(stream.previous()) { "match consumed the type token" }
        VersionFeatures.unavailable(typeToken, version)?.let { return it }
        return ASTResult.Success(typeToken)
    }

    private fun parseInitializer(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        isConst: Boolean,
        keywordToken: Token,
    ): ASTResult<Expression?> {
        if (stream.match(TokenType.ASSIGN)) {
            return expressionParser.parseExpression()
        }
        if (isConst) {
            return ASTResult.Failure("'const' requires an initializer.", keywordToken.start, keywordToken.end)
        }
        return ASTResult.Success(null)
    }
}
