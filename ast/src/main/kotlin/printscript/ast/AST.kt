package printscript.ast

import printscript.common.Position
import printscript.common.TokenType

interface PositionedNode {
    val position: Position
}

sealed interface Expression : PositionedNode {
    override val position: Position
}

data class BinaryExpression(
    val left: Expression,
    val operator: TokenType,
    val right: Expression,
    override val position: Position = Position(0, 0),
) : Expression

data class NumberLiteral(val value: Double, override val position: Position = Position(0, 0)) : Expression

data class StringLiteral(val value: String, override val position: Position = Position(0, 0)) : Expression

data class Identifier(val name: String, override val position: Position = Position(0, 0)) : Expression

sealed interface Statement : PositionedNode {
    override val position: Position
}

data class VariableDeclaration(
    val name: String,
    val type: String,
    val value: Expression?,
    override val position: Position = Position(0, 0),
) : Statement

data class Assignment(
    val name: String,
    val value: Expression,
    override val position: Position = Position(0, 0),
) : Statement

data class PrintCall(
    val value: Expression,
    override val position: Position = Position(0, 0),
) : Statement
