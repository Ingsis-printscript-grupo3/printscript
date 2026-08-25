package printscript.interpreter

import printscript.interpreter.output.BucketOutput
import printscript.interpreter.output.MultiOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutputTest {
    @Test
    fun `the bucket keeps the lines in order`() {
        val bucket = BucketOutput()

        bucket.emit("hello")
        bucket.emit("world")

        assertEquals(listOf("hello", "world"), bucket.lines())
    }

    @Test
    fun `a new bucket starts empty`() {
        assertTrue(BucketOutput().lines().isEmpty())
    }

    @Test
    fun `the list returned by lines does not change when the bucket keeps receiving`() {
        val bucket = BucketOutput()
        bucket.emit("hello")

        val before = bucket.lines()
        bucket.emit("world")

        assertEquals(listOf("hello"), before)
        assertEquals(listOf("hello", "world"), bucket.lines())
    }

    @Test
    fun `MultiOutput sends the same line to every destination`() {
        val a = BucketOutput()
        val b = BucketOutput()

        MultiOutput(a, b).emit("hello")

        assertEquals(listOf("hello"), a.lines())
        assertEquals(listOf("hello"), b.lines())
    }

    @Test
    fun `MultiOutput with no destinations does not fail`() {
        MultiOutput().emit("hello")
    }
}
