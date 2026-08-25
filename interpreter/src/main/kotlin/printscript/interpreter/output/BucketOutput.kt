package printscript.interpreter.output

// guarda c linea en memoria asi los tests pueden compararla c listas
class BucketOutput : Output {
    // el historial es inmutable: c emit arma una lista nueva
    private var history: List<String> = emptyList()

    override fun emit(line: String) {
        history = history + line
    }

    fun lines(): List<String> = history
}
