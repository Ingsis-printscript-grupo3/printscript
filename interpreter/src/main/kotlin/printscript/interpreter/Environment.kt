package printscript.interpreter

class Environment {
    private val variables = mutableMapOf<String, Value?>()

    fun declare(
        name: String,
        value: Value?,
    ) {
        if (variables.containsKey(name)) {
            throw VariableAlreadyDeclaredError(name)
        }
        variables[name] = value
    }

    fun assign(
        name: String,
        value: Value,
    ) {
        if (!variables.containsKey(name)) {
            throw UndeclaredVariableError(name)
        }
        variables[name] = value
    }

    fun lookup(name: String): Value {
        if (!variables.containsKey(name)) {
            throw UndeclaredVariableError(name)
        }
        return variables[name] ?: throw UninitializedVariableError(name)
    }
}
