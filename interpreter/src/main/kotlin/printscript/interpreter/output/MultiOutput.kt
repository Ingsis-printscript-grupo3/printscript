package printscript.interpreter.output

// manda la misma linea a varios destinos a la vez
class MultiOutput(vararg destinations: Output) : Output {
    // el vararg llega como Array, lo paso a List apenas entra
    private val outputs: List<Output> = destinations.toList()

    override fun emit(line: String) {
        outputs.forEach { it.emit(line) }
    }
}
