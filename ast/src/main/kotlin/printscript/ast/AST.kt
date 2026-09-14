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

data class BooleanLiteral(val value: Boolean, override val position: Position = Position(0, 0)) : Expression

data class ReadInput(val argument: Expression, override val position: Position = Position(0, 0)) : Expression

data class ReadEnv(val argument: Expression, override val position: Position = Position(0, 0)) : Expression

sealed interface Statement : PositionedNode {
    override val position: Position
}

// position apunta al let o const; namePosition al nombre, que es lo que el linter reporta.
// por defecto son la misma, asi quien arma el nodo a mano no tiene que pasar las dos
data class VariableDeclaration(
    val name: String,
    val type: String,
    val value: Expression?,
    override val position: Position = Position(0, 0),
    val isConst: Boolean = false,
    val namePosition: Position = position,
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

data class Block(
    val statements: List<Statement>,
    override val position: Position = Position(0, 0),
) : Statement

data class IfStatement(
    val condition: Expression,
    val thenBranch: Block,
    val elseBranch: Block?,
    override val position: Position = Position(0, 0),
) : Statement
