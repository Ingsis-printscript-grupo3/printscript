package printscript.semantic.symbol

import printscript.common.Position
import printscript.semantic.SemanticResult

data class VariableSymbol(
    val name: String,
    val type: String,
    val isConst: Boolean = false,
)

class SymbolTable {
    private val scopes: ArrayDeque<MutableMap<String, VariableSymbol>> =
        ArrayDeque<MutableMap<String, VariableSymbol>>().apply {
            addLast(mutableMapOf())
        }

    fun enterScope() {
        scopes.addLast(mutableMapOf())
    }

    fun exitScope() {
        check(scopes.size > 1) { "Cannot exit root scope" }
        scopes.removeLast()
    }

    // la posicion es la del nodo que pidio la operacion: la tabla solo la usa para que el error sepa donde ocurrio
    fun define(
        symbol: VariableSymbol,
        at: Position,
    ): SemanticResult<Unit> {
        val currentScope = scopes.last()
        if (currentScope.containsKey(symbol.name)) {
            return SemanticResult.Failure("Variable '${symbol.name}' already exists.", at)
        }
        currentScope[symbol.name] = symbol
        return SemanticResult.Success(Unit)
    }

    fun define(
        name: String,
        type: String,
        isConst: Boolean = false,
        at: Position,
    ): SemanticResult<Unit> = define(VariableSymbol(name, type, isConst), at)

    fun lookup(
        name: String,
        at: Position,
    ): SemanticResult<VariableSymbol> {
        for (i in scopes.indices.reversed()) {
            val scope = scopes[i]
            val symbol = scope[name]
            if (symbol != null) {
                return SemanticResult.Success(symbol)
            }
        }
        return SemanticResult.Failure("Variable '$name' not declared.", at)
    }

    fun lookupType(
        name: String,
        at: Position,
    ): SemanticResult<String> {
        return when (val res = lookup(name, at)) {
            is SemanticResult.Success -> SemanticResult.Success(res.value.type)
            is SemanticResult.Failure -> res
        }
    }
}
