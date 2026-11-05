package printscript.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ParseResult
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun `parses a variable declaration with a number`() {
        val statements = parseStatements("let x: number = 5;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        assertEquals("number", stmt.type)
        val value = stmt.value as NumberLiteral
        assertEquals(5.0, value.value)
    }

    @Test
    fun `parses a variable declaration with a string`() {
        val statements = parseStatements("let msg: string = \"hello\";")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("msg", stmt.name)
        assertEquals("string", stmt.type)
        val value = stmt.value as StringLiteral
        assertEquals("hello", value.value)
    }

    @Test
    fun `parses a variable declaration without an initializer`() {
        val statements = parseStatements("let x: number;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        assertEquals("number", stmt.type)
        assertEquals(null, stmt.value)
    }

    @Test
    fun `parses an assignment to an existing variable`() {
        val statements = parseStatements("x = 10;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as Assignment
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(10.0, value.value)
    }

    @Test
    fun `parses a println with a complex arithmetic expression`() {
        val statements = parseStatements("println((5 + 2) * 3);")

        assertEquals(1, statements.size)
        val stmt = statements[0] as PrintCall
        val binaryMult = stmt.value as BinaryExpression
        assertEquals(TokenType.MULTIPLY, binaryMult.operator)
        val binaryAdd = binaryMult.left as BinaryExpression
        assertEquals(TokenType.PLUS, binaryAdd.operator)
        assertEquals(5.0, (binaryAdd.left as NumberLiteral).value)
        assertEquals(2.0, (binaryAdd.right as NumberLiteral).value)
        assertEquals(3.0, (binaryMult.right as NumberLiteral).value)
    }

    @Test
    fun `parses a string concatenation`() {
        val statements = parseStatements("println(\"Result: \" + a);")

        assertEquals(1, statements.size)
        val stmt = statements[0] as PrintCall
        val binaryPlus = stmt.value as BinaryExpression
        assertEquals(TokenType.PLUS, binaryPlus.operator)
        assertEquals("Result: ", (binaryPlus.left as StringLiteral).value)
        assertEquals("a", (binaryPlus.right as Identifier).name)
    }

    @Test
    fun `fails when the semicolon is missing`() {
        val exception = assertThrows<SyntaxError> { parseStatements("println(5)") }
        assert(exception.message.contains("Expected ';'"))
    }

    @Test
    fun `fails when a closing parenthesis is missing in the expression`() {
        val exception = assertThrows<SyntaxError> { parseStatements("println((5 + 2 * 3);") }
        assert(exception.message.contains("Expected ')'"))
    }

    @Test
    fun `fails when the declaration has a number where the name goes`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let 5 : number = 5;") }
        assert(exception.message.contains("Expected variable name"))
    }

    @Test
    fun `fails when the declaration has no type`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let x: = 5;") }
        assert(exception.message.contains("Expected 'number', 'string' or 'boolean'"))
    }

    @Test
    fun `fails when the declaration has no type in version 1_0`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let x: = 5;", LanguageVersion.V1_0) }
        assert(exception.message.contains("Expected 'number' or 'string'."))
    }

    @Test
    fun `fails when the declaration has an unknown type in version 1_0`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let x: customType = 5;", LanguageVersion.V1_0) }
        assert(exception.message.contains("Expected 'number' or 'string'."))
    }

    // el lexer 1.0 no emite BOOLEANTYPE: se usan tokens de 1.1 para verificar que el parser 1.0 lo rechaza
    @Test
    fun `fails when the declaration has boolean type in version 1_0`() {
        val exception =
            assertThrows<SyntaxError> {
                parseStatements("let x: boolean;", LanguageVersion.V1_0, lexerVersion = LanguageVersion.V1_1)
            }
        assert(exception.message.contains("Expected 'number' or 'string'."))
    }

    @Test
    fun `fails when the declaration has an unknown type in version 1_1`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let x: customType = 5;") }
        assert(exception.message.contains("Expected 'number', 'string' or 'boolean'."))
    }

    @Test
    fun `parses number and string variable declarations in version 1_0`() {
        val statements = parseStatements("let n: number; let s: string;", LanguageVersion.V1_0)
        assertEquals(2, statements.size)
    }

    @Test
    fun `fails when input ends immediately after colon without EOF token in version 1_0`() {
        val colon = Token(TokenType.COLON, Position(1, 7), Position(1, 8), ":")
        val tokens =
            listOf(
                Token(TokenType.LET, Position(1, 1), Position(1, 4), "let"),
                Token(TokenType.IDENTIFIER, Position(1, 5), Position(1, 6), "x"),
                colon,
            )
        val parser = testParser(tokens.iterator(), LanguageVersion.V1_0)
        val results = parser.parse().asSequence().toList()
        assertEquals(1, results.size)
        val failure = results[0] as ParseResult.Failure
        assertEquals("Expected 'number' or 'string'.", failure.message)
        assertEquals(Position(1, 8), failure.start)
        assertEquals(Position(1, 8), failure.end)
    }

    @Test
    fun `fails when input ends immediately after colon without EOF token in version 1_1`() {
        val colon = Token(TokenType.COLON, Position(1, 7), Position(1, 8), ":")
        val tokens =
            listOf(
                Token(TokenType.LET, Position(1, 1), Position(1, 4), "let"),
                Token(TokenType.IDENTIFIER, Position(1, 5), Position(1, 6), "x"),
                colon,
            )
        val parser = testParser(tokens.iterator(), LanguageVersion.V1_1)
        val results = parser.parse().asSequence().toList()
        assertEquals(1, results.size)
        val failure = results[0] as ParseResult.Failure
        assertEquals("Expected 'number', 'string' or 'boolean'.", failure.message)
        assertEquals(Position(1, 8), failure.start)
        assertEquals(Position(1, 8), failure.end)
    }

    @Test
    fun `fails when the file starts with a token that begins no statement`() {
        val exception = assertThrows<SyntaxError> { parseStatements(")") }

        assert(exception.message.contains("Unexpected token ')'"))
    }

    @Test
    fun `fails when the input ends in the middle of an expression`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let x: number = 5 +") }

        assert(exception.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails when readInput is not closed`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let name: string = readInput(\"a\";") }

        assert(exception.message.contains("Expected ')' closing 'readInput'"))
    }

    @Test
    fun `parses a variable declaration with a negative number`() {
        val statements = parseStatements("let x: number = -5;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(-5.0, value.value)
    }

    @Test
    fun `parses a variable declaration with a negative float`() {
        val statements = parseStatements("let x: number = -3.14;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        val value = stmt.value as NumberLiteral
        assertEquals(-3.14, value.value)
    }

    @Test
    fun `parses a variable declaration with a negative identifier`() {
        val statements = parseStatements("let y: number = -x;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        val binary = stmt.value as BinaryExpression
        assertEquals(TokenType.MINUS, binary.operator)
        assertEquals(0.0, (binary.left as NumberLiteral).value)
        assertEquals("x", (binary.right as Identifier).name)
    }

    @Test
    fun `parses an expression with subtraction of negative number`() {
        val statements = parseStatements("let y: number = 5 - -3;")

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        val binary = stmt.value as BinaryExpression
        assertEquals(TokenType.MINUS, binary.operator)
        assertEquals(5.0, (binary.left as NumberLiteral).value)
        val right = binary.right as NumberLiteral
        assertEquals(-3.0, right.value)
    }

    @Test
    fun `fails when statement ends with trailing unary minus`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let x: number = -;") }

        assert(exception.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails when binary operation ends with trailing unary minus`() {
        val exception = assertThrows<SyntaxError> { parseStatements("let x: number = 5 + -;") }

        assert(exception.message.contains("Expected a value or expression"))
    }
}
