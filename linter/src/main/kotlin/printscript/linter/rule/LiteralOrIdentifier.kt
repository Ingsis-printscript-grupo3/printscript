package printscript.linter.rule

import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral

// la consigna pide lo mismo para println y para readInput: solo un identificador o un literal.
// Se evalua por pertenencia positiva para respetar el principio Open/Closed y evitar acoplamiento
// rigido con nuevas expresiones del lenguaje.
internal fun Expression.isLiteralOrIdentifier(): Boolean =
    this is Identifier || this is NumberLiteral || this is StringLiteral || this is BooleanLiteral
