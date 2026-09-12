# ADR 0001: Extensibilidad del recorrido del AST (semantic + interpreter)

## Estado

Aceptado.

## Contexto

`semantic` (`StatementValidator`, `ExpressionResolver`) y `interpreter` (`Interpreter`)
son, cada uno, un conjunto fijo de "operaciones" (validar, interpretar) que recorren un
AST cuyos tipos de nodo (`Statement`, `Expression`) van a seguir creciendo a medida que
el lenguaje suma features (`if`, `while`, funciones, etc.), y en paralelo, por distintos
devs del equipo.

Esto es el **problema de la expresión** (expression problem): un AST + un conjunto de
operaciones sobre él se puede modelar de forma que sea fácil agregar nodos nuevos sin
tocar las operaciones existentes, o de forma que sea fácil agregar operaciones nuevas sin
tocar los nodos existentes — pero no ambas cosas gratis al mismo tiempo con OO clásica:

- **`when`/`is` por tipo** (lo que había antes en `semantic`): agregar una operación es
  fácil (una función más), pero agregar un nodo obliga a tocar el `when` de cada
  operación existente (`StatementValidator`, `ExpressionResolver`, `Interpreter`,
  `Formatter`, `Linter`, ...).
- **Visitor** (`accept()` en cada nodo + un `Visitor` por operación): agregar una
  operación es agregar un `Visitor` nuevo sin tocar `ast`, pero agregar un nodo obliga a
  tocar `ast` (nuevo `accept()`) y **cada** `Visitor` existente.

## Decisión

Optimizamos el eje "agregar nodos", no el eje "agregar operaciones/herramientas":

- Las operaciones (`SemanticAnalyzer`, `Interpreter`, y a futuro `Formatter`, `Linter`)
  son pocas y cambian poco una vez escritas.
- Los nodos (`Statement`/`Expression`) van a crecer seguido, a mano de varios devs en
  paralelo, y cada uno toca un módulo distinto (`semantic`, `interpreter`, `formatter`,
  `linter`).

Por eso `semantic` pasa a usar el mismo patrón de **registro de handlers** que ya usan
`lexer` (`TokenReader`), `parser` (`StatementHandler`/`InfixParselet`) e `interpreter`
(`StatementInterpreter`/`ExpressionEvaluator`): una lista de plugins, cada uno con
`matches(node): Boolean`, recorrida con `firstOrNull { it.matches(node) }`.
`ExpressionResolver` es recursivo (`BinaryExpression` resuelve sus dos lados), así que
cada handler recibe una back-reference al resolver (`ExpressionResolverInterface`), igual
que `interpreter` le pasa `InterpreterInterface` a cada `ExpressionEvaluator`.

Agregar un nodo nuevo pasa a ser: crear una clase (`XxxHandler`/`XxxInterpreter`) y
agregarla a la lista default (`DefaultStatementHandlers`, `DefaultExpressionHandlers`,
`DefaultStatementInterpreters`, `DefaultExpressionEvaluators`). No hace falta tocar
`ast` ni el resto de los handlers.

**No usamos Visitor** porque exigiría agregar `accept()` a cada nodo de `ast`, y `ast`
pasaría a conocer (acoplarse a) la interfaz de Visitor de cada consumidor —
`semantic`, `interpreter`, `formatter`, `linter` — exactamente lo que queremos evitar:
que agregar un nodo obligue a tocar código fuera de donde se define el nodo. `ast` ya es
`sealed` y no necesita `accept()`.

## Consecuencias

**Lo que se gana:** agregar un tipo de nodo no rompe (en el sentido de "hay que editar")
ninguno de los módulos existentes que aún no lo soportan — se registra su handler donde
corresponda y listo. Duplicación eliminada: el guard "es mío / si no, exploto" que antes
vivía repetido en cada handler (`matches()` + `if (x !is T) throw ...`) ahora vive una
sola vez en la clase base genérica (`StatementHandler<T>`, `ExpressionHandler<T>`,
`StatementInterpreter<T>`, `ExpressionEvaluator<T>`): el tipo concreto va en la firma del
método típado y el cast/guard lo hace la clase base.

**Lo que se resigna:** con `when`/`is` el compilador de Kotlin fuerza (exhaustividad
sobre `sealed interface`) a actualizar cada validator/interpreter en el momento en que se
agrega un subtipo nuevo. Con el patrón de registro esa garantía desaparece: si alguien
agrega un `Statement`/`Expression` nuevo y se olvida de registrar su handler, el build
compila igual y el error recién aparece en runtime (`UnknownStatementError` /
`UnknownExpressionError`).

Como red de seguridad, cada módulo (`semantic`, `interpreter`) mantiene:

1. Un `when` exhaustivo **privado, sin `else`**, que no decide nada (ramas vacías) y cuyo
   único propósito es que el compilador deje de compilar el módulo el día que aparezca un
   subtipo nuevo — señal en tiempo de compilación de "hay que ir a registrar un handler
   acá". Ver `StatementValidator`/`ExpressionResolver` en `semantic` e `Interpreter` en
   `interpreter`.
2. Un test de completitud por módulo que recorre `Statement::class.sealedSubclasses` /
   `Expression::class.sealedSubclasses` (vía `kotlin-reflect`, solo en tests) y verifica
   que cada subtipo tenga un handler registrado en la lista default — red de seguridad
   adicional en caso de que el witness del punto 1 se borre o se edite mal.

Ninguna de las dos redes reemplaza la exhaustividad del compilador al 100%: ambas fallan
recién cuando se compila/testea el módulo, no en el lugar exacto donde falta el handler.
Es el costo que pagamos a cambio de no acoplar `ast` a sus consumidores.
