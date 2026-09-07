package printscript.interpreter.input

class QueueInput(inputs: List<String> = emptyList()) : InputProvider {
    private val queue: ArrayDeque<String> = ArrayDeque(inputs)

    constructor(vararg inputs: String) : this(inputs.toList())

    override fun readInput(prompt: String): String {
        if (queue.isEmpty()) {
            throw NoSuchElementException("No more inputs available in QueueInput")
        }
        return queue.removeFirst()
    }
}
