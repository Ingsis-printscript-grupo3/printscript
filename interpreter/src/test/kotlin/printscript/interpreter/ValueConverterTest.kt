package printscript.interpreter

import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// posicion de mentira: estos tests miran el error, no donde ocurrio
private val AT = Position(1, 1)

class ValueConverterTest {
    @Test
    fun `convert returns same value when type matches targetType`() {
        val num = NumberValue(42.0)
        val str = StringValue("hello")
        val bool = BooleanValue(true)

        assertEquals(num, ValueConverter.convert(num, "number", at = AT))
        assertEquals(str, ValueConverter.convert(str, "string", at = AT))
        assertEquals(bool, ValueConverter.convert(bool, "boolean", at = AT))
    }

    @Test
    fun `convert converts number and boolean to string`() {
        assertEquals(StringValue("42"), ValueConverter.convert(NumberValue(42.0), "string", at = AT))
        assertEquals(StringValue("3.14"), ValueConverter.convert(NumberValue(3.14), "string", at = AT))
        assertEquals(StringValue("true"), ValueConverter.convert(BooleanValue(true), "string", at = AT))
        assertEquals(StringValue("false"), ValueConverter.convert(BooleanValue(false), "string", at = AT))
    }

    @Test
    fun `convert converts valid strings to number`() {
        assertEquals(NumberValue(100.0), ValueConverter.convert(StringValue("100"), "number", at = AT))
        assertEquals(NumberValue(42.5), ValueConverter.convert(StringValue("42.5"), "number", at = AT))
        assertEquals(NumberValue(-7.25), ValueConverter.convert(StringValue("-7.25"), "number", at = AT))
    }

    @Test
    fun `convert throws ValueConversionError on invalid number conversion`() {
        val strError =
            assertFailsWith<ValueConversionError> {
                ValueConverter.convert(StringValue("not_a_number"), "number", at = AT)
            }
        assertEquals(StringValue("not_a_number"), strError.value)
        assertEquals("number", strError.targetType)
        assertEquals("Cannot convert value 'not_a_number' to type 'number'", strError.message)

        val boolError =
            assertFailsWith<ValueConversionError> {
                ValueConverter.convert(BooleanValue(true), "number", at = AT)
            }
        assertEquals(BooleanValue(true), boolError.value)
        assertEquals("number", boolError.targetType)
    }

    @Test
    fun `convert converts valid strings to boolean`() {
        assertEquals(BooleanValue(true), ValueConverter.convert(StringValue("true"), "boolean", at = AT))
        assertEquals(BooleanValue(false), ValueConverter.convert(StringValue("false"), "boolean", at = AT))
    }

    @Test
    fun `convert throws ValueConversionError on invalid boolean conversion`() {
        val strError =
            assertFailsWith<ValueConversionError> {
                ValueConverter.convert(StringValue("hello"), "boolean", at = AT)
            }
        assertEquals(StringValue("hello"), strError.value)
        assertEquals("boolean", strError.targetType)
        assertEquals("Cannot convert value 'hello' to type 'boolean'", strError.message)

        val numError =
            assertFailsWith<ValueConversionError> {
                ValueConverter.convert(NumberValue(1.0), "boolean", at = AT)
            }
        assertEquals(NumberValue(1.0), numError.value)
        assertEquals("boolean", numError.targetType)

        val caseSensitiveError =
            assertFailsWith<ValueConversionError> {
                ValueConverter.convert(StringValue("True"), "boolean", at = AT)
            }
        assertEquals(StringValue("True"), caseSensitiveError.value)
        assertEquals("boolean", caseSensitiveError.targetType)
    }

    @Test
    fun `convert throws ValueConversionError on unsupported target type`() {
        val error =
            assertFailsWith<ValueConversionError> {
                ValueConverter.convert(StringValue("val"), "custom_type", at = AT)
            }
        assertEquals(StringValue("val"), error.value)
        assertEquals("custom_type", error.targetType)
        assertEquals("Cannot convert value 'val' to type 'custom_type'", error.message)
    }
}
