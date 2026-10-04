package printscript.interpreter.input

class QueueInput(inputs: List<String> = emptyList()) : InputProvider {
    private val queue: ArrayDeque<String> = ArrayDeque(inputs)

    constructor(vararg inputs: String) : this(inputs.toList())

    override fun readInput(prompt: String): String? = queue.removeFirstOrNull()
}
