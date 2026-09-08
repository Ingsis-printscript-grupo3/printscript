# ADR 0003: Restricción de tipado en operaciones con booleanos

## Estado

Aceptado.

## Contexto

Con la introducción de PrintScript 1.1 (Ticket 2.2), se incorporan el tipo `boolean` y los literales `true`/`false`.
Surgió la necesidad de definir el comportamiento de las expresiones binarias ante operandos booleanos:
- En algunos lenguajes como JavaScript/TypeScript, el operador `+` permite la concatenación implícita de cadenas con booleanos (por ejemplo, `"flag: " + true` resulta en `"flag: true"`).
- Asimismo, podría plantearse la coerción de booleanos a números (`true + 1 -> 2`).

## Decisión

Se prohíbe explícitamente el uso de operandos de tipo `boolean` en todos los operadores binarios aritméticos y de concatenación (`+`, `-`, `*`, `/`).
- No se realiza coerción implícita de `boolean` a `string` ni a `number` en expresiones binarias (`"flag: " + true` resulta en un error semántico `Semantic Error: Operator '+' cannot be applied to boolean types.`).
- Para imprimir o representar valores booleanos, se utiliza directamente la llamada a `println(boolean)` o se controlan los flujos mediante sentencias condicionales `if (cond)`.
- Esta decisión favorece un sistema de tipos estático, explícito y predecible, alineado estrictamente con la especificación de PrintScript y evitando efectos colaterales de conversión automática de tipos.

## Consecuencias

- **Mayor robustez y previsibilidad**: Los errores de tipado con booleanos se detectan tempranamente en la fase de análisis semántico antes de alcanzar la ejecución en el intérprete.
- **Sin efectos colaterales**: El comportamiento de los operadores `+`, `-`, `*`, `/` se mantiene puro y reservado únicamente para las combinaciones válidas de `number` y `string`.
