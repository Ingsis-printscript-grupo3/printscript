package printscript.linter.rule

import printscript.ast.BinaryExpression
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.StringLiteral

// la consigna pide lo mismo para println y para readInput: solo un identificador o un literal.
// es una lista blanca a proposito, y sin else: una expresion nueva del lenguaje no compila
// hasta que alguien decida de que lado va, en vez de colarse o quedar afuera sin aviso
internal fun Expression.isLiteralOrIdentifier(): Boolean =
    when (this) {
        is Identifier, is NumberLiteral, is StringLiteral, is BooleanLiteral -> true
        is BinaryExpression, is ReadInput, is ReadEnv -> false
    }
