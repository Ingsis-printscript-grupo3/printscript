package printscript.semantic.symbol

import printscript.semantic.SemanticResult

sealed interface Symbol {
    val name: String
}

data class VariableSymbol(
    override val name: String,
    val type: String,
    val isConst: Boolean = false,
) : Symbol

class SymbolTable {
    private val scopes: ArrayDeque<MutableMap<String, Symbol>> =
        ArrayDeque<MutableMap<String, Symbol>>().apply {
            addLast(mutableMapOf())
        }

    fun enterScope() {
        scopes.addLast(mutableMapOf())
    }

    fun exitScope() {
        check(scopes.size > 1) { "Cannot exit root scope" }
        scopes.removeLast()
    }

    fun define(symbol: Symbol): SemanticResult<Unit> {
        val currentScope = scopes.last()
        if (currentScope.containsKey(symbol.name)) {
            return SemanticResult.Failure("Semantic Error: Variable '${symbol.name}' already exists.")
        }
        currentScope[symbol.name] = symbol
        return SemanticResult.Success(Unit)
    }

    fun define(
        name: String,
        type: String,
        isConst: Boolean = false,
    ): SemanticResult<Unit> = define(VariableSymbol(name, type, isConst))

    fun lookup(name: String): SemanticResult<Symbol> {
        for (i in scopes.indices.reversed()) {
            val scope = scopes[i]
            val symbol = scope[name]
            if (symbol != null) {
                return SemanticResult.Success(symbol)
            }
        }
        return SemanticResult.Failure("Semantic Error: Variable '$name' not declared.")
    }

    fun lookupVariable(name: String): SemanticResult<VariableSymbol> {
        return when (val res = lookup(name)) {
            is SemanticResult.Success -> {
                val sym = res.value
                if (sym is VariableSymbol) {
                    SemanticResult.Success(sym)
                } else {
                    SemanticResult.Failure("Semantic Error: '$name' is not a variable.")
                }
            }
            is SemanticResult.Failure -> res
        }
    }

    fun lookupType(name: String): SemanticResult<String> {
        return when (val res = lookupVariable(name)) {
            is SemanticResult.Success -> SemanticResult.Success(res.value.type)
            is SemanticResult.Failure -> res
        }
    }
}
