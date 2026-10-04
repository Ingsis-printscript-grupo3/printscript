package printscript.interpreter.input

interface InputProvider {
    // null cuando no hay mas datos para dar: el interprete lo convierte en MissingInputError
    fun readInput(prompt: String): String?
}
