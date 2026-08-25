package printscript.interpreter.output

// imprime en consola y no guarda nada
class ConsoleOutput : Output {
    override fun emit(line: String) {
        println(line)
    }
}
