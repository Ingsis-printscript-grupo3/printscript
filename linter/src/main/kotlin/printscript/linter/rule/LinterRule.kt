package printscript.linter.rule

import printscript.ast.PositionedNode
import printscript.linter.Warning

/**
 * Regla de analisis estatico sobre nodos del AST.
 */
interface LinterRule {
    /**
     * Determina si la regla tiene interes en analizar el nodo dado.
     */
    fun appliesTo(node: PositionedNode): Boolean

    /**
     * Analiza el nodo y retorna la lista de advertencias encontradas.
     * Si se invoca sobre un nodo no aplicable, debe retornar una lista vacia de forma segura sin lanzar excepciones.
     */
    fun check(node: PositionedNode): List<Warning>
}
