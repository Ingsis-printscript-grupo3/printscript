package printscript.linter.rule

import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral

// la consigna pide lo mismo para println y para readInput: solo un identificador o un literal.
// es una lista blanca a proposito: cualquier expresion nueva del lenguaje queda afuera hasta
// que alguien decida lo contrario, en vez de colarse por no estar en una lista negra
internal fun Expression.isLiteralOrIdentifier(): Boolean =
    this is Identifier ||
        this is NumberLiteral ||
        this is StringLiteral ||
        this is BooleanLiteral
