package printscript.ast

import printscript.common.TokenType

sealed interface Expression

data class BinaryExpression(
    val left: Expression,
    val operator: TokenType,
    val right: Expression
) : Expression

data class NumberLiteral(val value: Double) : Expression

data class StringLiteral(val value: String) : Expression

data class Identifier(val name: String) : Expression

sealed interface Statement

data class VariableDeclaration(
    val name: String,
    val type: String,
    val value: Expression?
) : Statement

data class Assignment(
    val name: String,
    val value: Expression
) : Statement

data class PrintCall(
    val value: Expression
) : Statement
