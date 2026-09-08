package printscript.semantic.handler

import printscript.ast.Assignment
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult
import printscript.semantic.StatementValidator
import printscript.semantic.handler.expression.BinaryExpressionHandler
import printscript.semantic.handler.expression.BooleanLiteralHandler
import printscript.semantic.handler.expression.IdentifierHandler
import printscript.semantic.handler.expression.NumberLiteralHandler
import printscript.semantic.handler.expression.ReadEnvHandler
import printscript.semantic.handler.expression.ReadInputHandler
import printscript.semantic.handler.expression.StringLiteralHandler
import printscript.semantic.handler.statement.AssignmentHandler
import printscript.semantic.handler.statement.BlockHandler
import printscript.semantic.handler.statement.IfStatementHandler
import printscript.semantic.handler.statement.PrintCallHandler
import printscript.semantic.handler.statement.VariableDeclarationHandler
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertIs

// cada handler tiene un guard que devuelve Failure si le llega un nodo que no es suyo
// por el flujo normal nunca pasa, pq el Registry pregunta applies() antes

class HandlerGuardsTest {
    private fun resolverContext() = ExpressionResolver(SymbolTable(), LanguageVersion.V1_1)

    private fun validatorContext(): StatementValidator {
        val symbolTable = SymbolTable()
        return StatementValidator(
            symbolTable,
            ExpressionResolver(symbolTable, LanguageVersion.V1_1),
            LanguageVersion.V1_1,
        )
    }

    @Test
    fun `expression handlers reject nodes that are not theirs`() {
        val number = NumberLiteral(1.0)
        val string = StringLiteral("hi")

        val cases =
            listOf(
                NumberLiteralHandler() to string,
                StringLiteralHandler() to number,
                BooleanLiteralHandler() to number,
                IdentifierHandler() to number,
                BinaryExpressionHandler() to number,
                ReadInputHandler() to number,
                ReadEnvHandler() to number,
            )

        for ((handler, foreignNode) in cases) {
            assertIs<SemanticResult.Failure>(handler.handle(foreignNode, resolverContext()))
        }
    }

    @Test
    fun `statement handlers reject nodes that are not theirs`() {
        val print = PrintCall(NumberLiteral(1.0))
        val assignment = Assignment("x", NumberLiteral(1.0))

        val cases =
            listOf(
                VariableDeclarationHandler() to print,
                AssignmentHandler() to print,
                PrintCallHandler() to assignment,
                IfStatementHandler() to print,
                BlockHandler() to print,
            )

        for ((handler, foreignNode) in cases) {
            assertIs<SemanticResult.Failure>(handler.handle(foreignNode, validatorContext()))
        }
    }

    @Test
    fun `identifier handler looks up the symbol table from the context`() {
        val symbolTable = SymbolTable()
        symbolTable.define("a", "string")
        val resolver = ExpressionResolver(symbolTable, LanguageVersion.V1_1)

        val result = IdentifierHandler().handle(Identifier("a"), resolver)

        assertIs<SemanticResult.Success<String>>(result)
    }

    @Test
    fun `variable declaration handler uses the symbol table from the context`() {
        val validator = validatorContext()

        val result = VariableDeclarationHandler().handle(VariableDeclaration("a", "number", null), validator)

        assertIs<SemanticResult.Success<Unit>>(result)
    }
}
