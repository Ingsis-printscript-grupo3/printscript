package printscript.runner

import kotlin.test.Test
import kotlin.test.assertSame

class ErrorInfoTest {
    // el TCK corre con 6m de heap y espera que un OutOfMemoryError llegue reportado, no propagado.
    // Para poder reportarlo no se puede crear ningun objeto: ya no hay memoria. Estos dos asserts
    // fallan si alguien vuelve a construir el error en el momento.
    @Test
    fun `the out of memory error is preallocated, not built on the spot`() {
        assertSame(describe(OutOfMemoryError()), describe(OutOfMemoryError()))
    }

    @Test
    fun `the out of memory result is preallocated too`() {
        assertSame(
            describe(OutOfMemoryError()).asExecutionResult(),
            describe(OutOfMemoryError()).asExecutionResult(),
        )
    }
}
