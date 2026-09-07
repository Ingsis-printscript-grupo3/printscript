package printscript.interpreter.input

interface InputProvider {
    fun readInput(prompt: String): String
}
