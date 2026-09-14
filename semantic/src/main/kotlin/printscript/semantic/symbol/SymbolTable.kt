package printscript.semantic.symbol

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

    fun define(symbol: VariableSymbol): SemanticResult<Unit> {
        val currentScope = scopes.last()
        if (currentScope.containsKey(symbol.name)) {
            return SemanticResult.Failure("Variable '${symbol.name}' already exists.")
        }
        currentScope[symbol.name] = symbol
        return SemanticResult.Success(Unit)
    }

    fun define(
        name: String,
        type: String,
        isConst: Boolean = false,
    ): SemanticResult<Unit> = define(VariableSymbol(name, type, isConst))

    fun lookup(name: String): SemanticResult<VariableSymbol> {
        for (i in scopes.indices.reversed()) {
            val scope = scopes[i]
            val symbol = scope[name]
            if (symbol != null) {
                return SemanticResult.Success(symbol)
            }
        }
        return SemanticResult.Failure("Variable '$name' not declared.")
    }

    fun lookupType(name: String): SemanticResult<String> {
        return when (val res = lookup(name)) {
            is SemanticResult.Success -> SemanticResult.Success(res.value.type)
            is SemanticResult.Failure -> res
        }
    }
}
