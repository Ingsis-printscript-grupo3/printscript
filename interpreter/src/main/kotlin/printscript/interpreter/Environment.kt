package printscript.interpreter

import printscript.common.Position

// solo guarda variables por scope. La posicion que recibe es la del nodo que pidio la operacion,
// y la usa unicamente para que el error sepa donde ocurrio
class Environment {
    private data class Binding(
        var value: Value?,
        val type: String?,
        val isConst: Boolean,
    )

    private val scopes =
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
        at: Position,
    ) {
        val currentScope = scopes.last()
        if (currentScope.containsKey(name)) {
            throw VariableAlreadyDeclaredError(name, at)
        }
        currentScope[name] = Binding(value, type, isConst)
    }

    fun assign(
        name: String,
        value: Value,
        at: Position,
    ) {
        val binding = find(name) ?: throw UndeclaredVariableError(name, at)
        if (binding.isConst) {
            throw CannotAssignToConstError(name, at)
        }
        binding.value = value
    }

    fun lookup(
        name: String,
        at: Position,
    ): Value {
        val binding = find(name) ?: throw UndeclaredVariableError(name, at)
        return binding.value ?: throw UninitializedVariableError(name, at)
    }

    fun typeOf(name: String): String? = find(name)?.type

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
