package printscript.parser.statement

import printscript.ast.*
import printscript.common.TokenType
import printscript.parser.stream.TokenStream
import printscript.parser.expression.ExpressionParser

class StatementParser(
    private val stream: TokenStream,
    private val expressionParser: ExpressionParser
) {

    fun parseStatement(): Statement {
        if (stream.match(TokenType.LET)) return parseDeclaration()
        if (stream.match(TokenType.PRINTLN)) return parsePrintCall()
        if (stream.match(TokenType.IDENTIFIER)) return parseAssignment()

        val errorToken = stream.peek()
        throw RuntimeException("Syntax Error [Line ${errorToken.start.line}]: Unexpected token '${errorToken.value}'.")
    }

    private fun parseDeclaration(): Statement {
        val nameToken = stream.consume(TokenType.IDENTIFIER, "Expected variable name.")
        stream.consume(TokenType.COLON, "Expected ':'.")
        val typeToken = if (stream.match(TokenType.NUMBERTYPE, TokenType.STRINGTYPE)) stream.previous() else throw RuntimeException("Syntax Error: Expected 'number' or 'string'.")

        var initializer: Expression? = null
        if (stream.match(TokenType.ASSIGN)) {
            initializer = expressionParser.parseExpression()
        }

        stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        return VariableDeclaration(nameToken.value, typeToken.value, initializer)
    }

    private fun parsePrintCall(): Statement {
        stream.consume(TokenType.LEFTPAREN, "Expected '('.")
        val value = expressionParser.parseExpression()
        stream.consume(TokenType.RIGHTTPAREN, "Expected ')'.")
        stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        return PrintCall(value)
    }

    private fun parseAssignment(): Statement {
        val nameToken = stream.previous()
        stream.consume(TokenType.ASSIGN, "Expected '='.")
        val value = expressionParser.parseExpression()
        stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        return Assignment(nameToken.value, value)
    }
}
