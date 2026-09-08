# ADR 0003: Restricción de tipado en operaciones con booleanos y shadowing léxico

## Estado

Aceptado.

## Contexto

Con la introducción de PrintScript 1.1 (Ticket 2.2), se incorporan el tipo `boolean`, literales `true`/`false`, sentencias condicionales `if` y bloques con scopes léxicos.
Surgieron dos decisiones de diseño relevantes para el módulo `semantic`:

1. **Operaciones binarias con booleanos**: El operador `+` admite suma de números y concatenación de cadenas con números (`string + number`, `number + string`). TypeScript permite concatenar booleanos con strings (`"flag: " + true -> "flag: true"`).
2. **Shadowing léxico**: Declarar una variable dentro de un bloque anidado (`thenBranch`, `elseBranch` o `Block`) con el mismo identificador que una variable de un scope superior.

## Decisión

### 1. Tipado estricto en operaciones binarias
Se prohíbe explícitamente el uso de expresiones de tipo `boolean` en todos los operadores binarios aritméticos (`+`, `-`, `*`, `/`).
- No se realiza coerción implícita de `boolean` a `string` en expresiones binarias (`"flag: " + true` resulta en error semántico).
- Para imprimir booleanos, se utiliza directamente `println(boolean)` o se manipulan mediante sentencias condicionales `if (flag)`.
- Esta decisión favorece un sistema de tipos estático y explícito, evitando conversiones implícitas no documentadas en la especificación del lenguaje.

### 2. Shadowing léxico en ámbitos anidados
Se permite el shadowing léxico de variables:
- Una declaración `let` o `const` dentro de un bloque anidado puede reutilizar un nombre previamente declarado en un ámbito exterior.
- La búsqueda en `SymbolTable` se realiza de manera inversa (desde el scope actual hacia los ancestros), resolviendo siempre la definición más cercana.
- El ciclo de vida del scope anidado está protegido por `try/finally { symbolTable.exitScope() }` en `BlockHandler`, asegurando que al salir del bloque la variable externa recupere su visibilidad intacta.

## Consecuencias

- **Mayor robustez y previsibilidad**: Los errores de tipado con booleanos se detectan tempranamente en la fase semántica antes de alcanzar el intérprete.
- **Aislamiento de ámbitos**: Los bloques pueden declarar identificadores temporales sin riesgo de colisión accidental o mutación del estado del ámbito contenedor.
