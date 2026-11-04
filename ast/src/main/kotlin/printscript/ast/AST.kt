package printscript.ast

import printscript.common.Position
import printscript.common.TokenType

interface PositionedNode {
    val position: Position

    // sin default a proposito: un nodo nuevo no compila hasta declarar sus hijos,
    // y los declara en su propia clase, sin tocar los recorridos que ya existen
    val children: List<PositionedNode>
}

sealed interface Expression : PositionedNode {
    override val position: Position
}

data class BinaryExpression(
    val left: Expression,
    val operator: TokenType,
    val right: Expression,
    override val position: Position = Position.UNKNOWN,
) : Expression {
    override val children: List<PositionedNode> get() = listOf(left, right)
}

data class NumberLiteral(val value: Double, override val position: Position = Position.UNKNOWN) : Expression {
    override val children: List<PositionedNode> get() = emptyList()
}

data class StringLiteral(val value: String, override val position: Position = Position.UNKNOWN) : Expression {
    override val children: List<PositionedNode> get() = emptyList()
}

data class Identifier(val name: String, override val position: Position = Position.UNKNOWN) : Expression {
    override val children: List<PositionedNode> get() = emptyList()
}

data class BooleanLiteral(val value: Boolean, override val position: Position = Position.UNKNOWN) : Expression {
    override val children: List<PositionedNode> get() = emptyList()
}

data class ReadInput(val argument: Expression, override val position: Position = Position.UNKNOWN) : Expression {
    override val children: List<PositionedNode> get() = listOf(argument)
}

data class ReadEnv(val argument: Expression, override val position: Position = Position.UNKNOWN) : Expression {
    override val children: List<PositionedNode> get() = listOf(argument)
}

sealed interface Statement : PositionedNode {
    override val position: Position
}

// position apunta al let o const; namePosition al nombre, que es lo que el linter reporta.
// por defecto son la misma, asi quien arma el nodo a mano no tiene que pasar las dos
data class VariableDeclaration(
    val name: String,
    val type: String,
    val value: Expression?,
    override val position: Position = Position.UNKNOWN,
    val isConst: Boolean = false,
    val namePosition: Position = position,
) : Statement {
    override val children: List<PositionedNode> get() = listOfNotNull(value)
}

data class Assignment(
    val name: String,
    val value: Expression,
    override val position: Position = Position.UNKNOWN,
) : Statement {
    override val children: List<PositionedNode> get() = listOf(value)
}

data class PrintCall(
    val value: Expression,
    override val position: Position = Position.UNKNOWN,
) : Statement {
    override val children: List<PositionedNode> get() = listOf(value)
}

data class Block(
    val statements: List<Statement>,
    override val position: Position = Position.UNKNOWN,
) : Statement {
    override val children: List<PositionedNode> get() = statements
}

data class IfStatement(
    val condition: Expression,
    val thenBranch: Block,
    val elseBranch: Block?,
    override val position: Position = Position.UNKNOWN,
) : Statement {
    override val children: List<PositionedNode> get() = listOfNotNull(condition, thenBranch, elseBranch)
}
