package printscript.semantic.symbol

class SymbolTable {
    private val symbols = mutableMapOf<String, String>()

    fun define(name: String, type: String) {
        if (symbols.containsKey(name)) {
            throw RuntimeException("Semantic Error: Variable '$name' already exists.")
        }
        symbols[name] = type
    }

    fun lookup(name: String): String {
        return symbols[name]
            ?: throw RuntimeException("Semantic Error: Variable '$name' not declared.")
    }
}
