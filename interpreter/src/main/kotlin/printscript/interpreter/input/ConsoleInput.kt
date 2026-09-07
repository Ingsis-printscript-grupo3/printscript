package printscript.interpreter.input

class ConsoleInput : InputProvider {
    override fun readInput(prompt: String): String = readlnOrNull() ?: ""
}
