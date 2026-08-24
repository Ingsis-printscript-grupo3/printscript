package printscript.semantic.symbol

import printscript.semantic.SemanticResult

class SymbolTable {
    private val symbols = mutableMapOf<String, String>()

    fun define(
        name: String,
        type: String,
    ): SemanticResult<Unit> {
        if (symbols.containsKey(name)) {
            return SemanticResult.Failure("Semantic Error: Variable '$name' already exists.")
        }
        symbols[name] = type
        return SemanticResult.Success(Unit)
    }

    fun lookup(name: String): SemanticResult<String> {
        return symbols[name]?.let { SemanticResult.Success(it) }
            ?: SemanticResult.Failure("Semantic Error: Variable '$name' not declared.")
    }
}
