package printscript.interpreter

class Environment {
    private data class Binding(
        var value: Value?,
        val type: String?,
        val isConst: Boolean,
    )

    private val scopes: ArrayDeque<MutableMap<String, Binding>> =
        ArrayDeque<MutableMap<String, Binding>>().apply {
            addLast(mutableMapOf())
        }

    fun enterScope() {
        scopes.addLast(mutableMapOf())
    }

    fun exitScope() {
        check(scopes.size > 1) { "Cannot exit root scope" }
        scopes.removeLast()
    }

    /** Declares a variable in the current lexical scope. */
    fun declare(
        name: String,
        value: Value?,
        type: String? = null,
        isConst: Boolean = false,
    ) {
        val currentScope = scopes.last()
        if (currentScope.containsKey(name)) {
            throw VariableAlreadyDeclaredError(name)
        }
        currentScope[name] = Binding(value, type, isConst)
    }

    fun assign(
        name: String,
        value: Value,
    ) {
        val binding = find(name) ?: throw UndeclaredVariableError(name)
        if (binding.isConst) {
            throw CannotAssignToConstError(name)
        }
        binding.value = value
    }

    fun lookup(name: String): Value {
        val binding = find(name) ?: throw UndeclaredVariableError(name)
        return binding.value ?: throw UninitializedVariableError(name)
    }

    fun typeOf(name: String): String? = find(name)?.type

    fun isConst(name: String): Boolean = find(name)?.isConst ?: false

    fun isDeclared(name: String): Boolean = find(name) != null

    private fun find(name: String): Binding? {
        for (i in scopes.indices.reversed()) {
            val scope = scopes[i]
            if (scope.containsKey(name)) {
                return scope[name]
            }
        }
        return null
    }
}
