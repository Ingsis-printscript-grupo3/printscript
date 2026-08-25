package printscript.parser.statement

import org.junit.jupiter.api.Test
import printscript.ast.Assignment
import printscript.ast.NumberLiteral
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Demuestra que se puede cambiar/agregar comportamiento de parseo de statements
 * sin modificar StatementParser: alcanza con inyectar un mapa de handlers distinto.
 */
class StatementParserExtensibilityTest {

    private fun pos() = Position(1, 1)
    private fun token(type: TokenType, value: String = "") = Token(type, pos(), pos(), value)

    @Test
    fun `custom handler registrado reemplaza el comportamiento por defecto sin tocar StatementParser`() {
        // Handler "plugin": ante un LET, en vez de parsear una declaracion real,
        // devuelve directamente una Assignment ficticia. No existe en DefaultStatementHandlers.
        val fakeHandler = StatementHandler { _, _ ->
            ASTResult.Success(Assignment("injected", NumberLiteral(1.0)))
        }

        val tokens = listOf(token(TokenType.LET), token(TokenType.EOF)).iterator()
        val stream = TokenStream(tokens)
        val expressionParser = ExpressionParser(stream)

        val statementParser = StatementParser(
            stream,
            expressionParser,
            handlers = mapOf(TokenType.LET to fakeHandler)
        )

        val result = statementParser.parseStatement()

        assertIs<ASTResult.Success<*>>(result)
        val statement = result.value
        assertIs<Assignment>(statement)
        assertEquals("injected", statement.name)
    }
}
