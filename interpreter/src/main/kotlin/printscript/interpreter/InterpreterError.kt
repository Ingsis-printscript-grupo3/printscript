package printscript.interpreter

sealed class InterpreterError(message: String) : RuntimeException(message)

class VariableAlreadyDeclaredError(val name: String) :
    InterpreterError("La variable '$name' ya fue declarada")

class UndeclaredVariableError(val name: String) :
    InterpreterError("La variable '$name' no fue declarada")

class UninitializedVariableError(val name: String) :
    InterpreterError("La variable '$name' fue declarada pero todavia no tiene un valor asignado")
