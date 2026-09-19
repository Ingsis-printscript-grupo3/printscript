package printscript.common

data class Position(val line: Int, val column: Int) {
    companion object {
        // las lineas y columnas arrancan en 1, asi que 0,0 no es una posicion real:
        // es lo que lleva un nodo armado a mano, sin un fuente detras
        val UNKNOWN = Position(0, 0)
    }
}
