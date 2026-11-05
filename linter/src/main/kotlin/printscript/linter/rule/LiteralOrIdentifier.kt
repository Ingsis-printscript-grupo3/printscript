package printscript.linter.rule

import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral

// la consigna pide lo mismo para println y para readInput: solo un identificador o un literal.
// es una lista blanca: una expresion nueva queda como no permitida hasta que se agregue aca.
// no hace falta que el compilador obligue como con children: si era un literal, se nota
// enseguida porque el linter avisa de mas, en vez de fallar en silencio
internal fun Expression.isLiteralOrIdentifier(): Boolean =
    this is Identifier || this is NumberLiteral || this is StringLiteral || this is BooleanLiteral
