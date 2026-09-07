package printscript.parser.statement.handlers

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionGate

class VariableDeclarationHandler(private val version: LanguageVersion) : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement> {
        val keywordToken =
            stream.previous()
                ?: return ASTResult.Failure("Expected 'let' or 'const'.", Position(0, 0), Position(0, 0))
        val isConst = keywordToken.type == TokenType.CONST
        if (isConst) {
            VersionGate.check("const declarations", LanguageVersion.V1_1, version, keywordToken.start, keywordToken.end)
                ?.let { return it }
        }

        val nameTokenResult = stream.consume(TokenType.IDENTIFIER, "Expected variable name.")
        if (nameTokenResult is ASTResult.Failure) return nameTokenResult
        val nameToken = (nameTokenResult as ASTResult.Success).value

        val colonResult = stream.consume(TokenType.COLON, "Expected ':'.")
        if (colonResult is ASTResult.Failure) return colonResult

        val typeResult = parseType(stream)
        if (typeResult is ASTResult.Failure) return typeResult
        val typeToken = (typeResult as ASTResult.Success).value

        val initializerResult = parseInitializer(stream, expressionParser, isConst, keywordToken)
        if (initializerResult is ASTResult.Failure) return initializerResult
        val initializer = (initializerResult as ASTResult.Success).value

        val semiResult = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semiResult is ASTResult.Failure) return semiResult

        return ASTResult.Success(
            VariableDeclaration(nameToken.value, typeToken.value, initializer, keywordToken.start, isConst),
        )
    }

    private fun parseType(stream: TokenStream): ASTResult<Token> {
        if (!stream.match(TokenType.NUMBERTYPE, TokenType.STRINGTYPE, TokenType.BOOLEANTYPE)) {
            val errorToken = stream.peek()
            val pos = errorToken?.start ?: stream.previous()?.end ?: Position(0, 0)
            return ASTResult.Failure("Expected 'number', 'string' or 'boolean'.", pos, errorToken?.end ?: pos)
        }
        val typeToken =
            stream.previous()
                ?: return ASTResult.Failure("Expected a type.", Position(0, 0), Position(0, 0))
        if (typeToken.type == TokenType.BOOLEANTYPE) {
            VersionGate.check("boolean type", LanguageVersion.V1_1, version, typeToken.start, typeToken.end)
                ?.let { return it }
        }
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
