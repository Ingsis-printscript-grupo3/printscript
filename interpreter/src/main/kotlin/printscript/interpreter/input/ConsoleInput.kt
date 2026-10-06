package printscript.interpreter.input

class ConsoleInput : InputProvider {
    // al terminarse stdin (EOF) devuelve null, igual que QueueInput vacio: el interprete tira MissingInputError
    override fun readInput(prompt: String): String? {
        if (prompt.isNotEmpty()) {
            print(prompt)
        }
        return readlnOrNull()
    }
}
