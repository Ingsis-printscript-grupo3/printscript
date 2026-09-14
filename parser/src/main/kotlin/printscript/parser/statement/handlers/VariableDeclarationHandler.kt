package printscript.parser.statement.handlers

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.result.unwrap
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionFeatures

object VariableDeclarationHandler : StatementHandler {
    private data class VarHeader(val nameToken: Token, val typeToken: Token)

    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement> {
        val keyword = checkNotNull(stream.previous()) { "the declaration handler runs after let or const" }
        val header = parseHeader(stream, statementParser.version).unwrap { return it }
        val isConst = keyword.type == TokenType.CONST
        val init = parseInitializer(stream, expressionParser, isConst, keyword).unwrap { return it }
        val semi = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semi is ASTResult.Failure) return semi

        return ASTResult.Success(buildDeclaration(keyword, header, init))
    }

    private fun parseHeader(
        stream: TokenStream,
        version: LanguageVersion,
    ): ASTResult<VarHeader> {
        val nameToken = stream.consume(TokenType.IDENTIFIER, "Expected variable name.").unwrap { return it }
        val colon = stream.consume(TokenType.COLON, "Expected ':'.")
        if (colon is ASTResult.Failure) return colon
        val typeToken = parseType(stream, version).unwrap { return it }
        return ASTResult.Success(VarHeader(nameToken, typeToken))
    }

    private fun buildDeclaration(
        keywordToken: Token,
        header: VarHeader,
        initializer: Expression?,
    ): VariableDeclaration =
        VariableDeclaration(
            name = header.nameToken.value,
            type = header.typeToken.value,
            value = initializer,
            position = keywordToken.start,
            isConst = keywordToken.type == TokenType.CONST,
            namePosition = header.nameToken.start,
        )

    private fun parseType(
        stream: TokenStream,
        version: LanguageVersion,
    ): ASTResult<Token> {
        if (!stream.match(TokenType.NUMBERTYPE, TokenType.STRINGTYPE, TokenType.BOOLEANTYPE)) {
            val errorToken = stream.peek()
            val pos = errorToken?.start ?: checkNotNull(stream.previous()) { "the type follows a ':'" }.end
            return ASTResult.Failure("Expected 'number', 'string' or 'boolean'.", pos, errorToken?.end ?: pos)
        }
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
