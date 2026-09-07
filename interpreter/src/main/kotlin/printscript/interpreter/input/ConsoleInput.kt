package printscript.interpreter.input

class ConsoleInput : InputProvider {
    override fun readInput(prompt: String): String {
        if (prompt.isNotEmpty()) {
            print(prompt)
        }
        return readlnOrNull() ?: ""
    }
}
