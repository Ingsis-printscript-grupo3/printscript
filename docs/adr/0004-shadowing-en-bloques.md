# ADR 0004: Shadowing léxico y ciclo de vida de ámbitos en bloques

## Estado

Aceptado.

## Contexto

Con la introducción de PrintScript 1.1 (Ticket 2.2), se incorporan las sentencias condicionales `if` / `else` y las secuencias de sentencias agrupadas en bloques (`Block`), cada una delimitada por llaves `{ ... }`.
Surgió la necesidad de definir las reglas de visibilidad, ciclo de vida de los ámbitos léxicos (scopes) y si se permite o no la re-declaración de identificadores ya existentes en ámbitos exteriores (shadowing).

## Decisión

1. **Soporte de Shadowing Léxico**:
   - Se permite que una sentencia de declaración (`let` o `const`) dentro de un bloque anidado declare una variable con el mismo nombre que una variable perteneciente a un ámbito contenedor superior.
   - La búsqueda en `SymbolTable` se realiza jerárquicamente de manera inversa (desde el scope actual local hacia los scopes ancestros/global), resolviendo siempre la definición más cercana.
2. **Ciclo de vida del Scope y Aislamiento**:
   - Cada bloque ejecuta `symbolTable.enterScope()` al inicio y `symbolTable.exitScope()` al finalizar.
   - Para garantizar que ningún fallo de validación o excepción deje la tabla de símbolos en un estado inconsistente, la salida del scope se resguarda dentro de un bloque `try/finally` en `BlockHandler`.
   - Al finalizar el bloque, las variables locales son descartadas y la variable externa del ámbito superior recupera su visibilidad original intacta.

## Consecuencias

- **Aislamiento de ámbitos**: Los bloques pueden declarar identificadores temporales sin riesgo de colisión accidental ni modificación inadvertida del estado del ámbito superior.
- **Robustez garantizada**: El uso de `try/finally` asegura que la jerarquía de scopes se mantenga perfectamente equilibrada aún ante la presencia de errores sintácticos o semánticos durante el análisis en streaming.
