# ADR 0002: Streaming del pipeline y el costo del error tardío

## Estado

Aceptado.

## Contexto

La consigna es explícita: *"los códigos fuentes pueden ser tan extensos que no es posible
contenerlos de forma completa en memoria (...) no se puede primero hacer todo el análisis
léxico para luego pasar al análisis semántico ya que el primero consumiría toda la memoria
disponible"*. El fuente tiene que **fluir** entre etapas, statement por statement.

La estructura para eso ya estaba: `lexer`, `parser`, `semantic` e `interpreter` exponen
`Iterator` y no colecciones; `CharStream` mantiene un lookahead de un caracter y
`TokenStream` de un token. Faltaban dos cosas:

1. **El buffering dependía de quién construía el `CharStream`.** `CharStream(reader)`
   llamaba `reader.read()` directo. Con un `StringReader` (los tests, el CLI de hoy) eso es
   gratis, pero con el `FileReader` que hace falta para no materializar el archivo es una
   llamada al sistema por caracter. El componente andaba bien o mal según el cuidado del
   llamador — o sea, no andaba bien.
2. **No había evidencia.** "El pipeline es lazy" era una propiedad que se sostenía leyendo
   el código. Nada impedía que un `readText()` o un `.toList()` la rompiera en silencio: no
   hay test que falle, ni error de compilación, y el síntoma recién aparece con un archivo
   grande de verdad.

## Decisión

**El buffering es responsabilidad del `CharStream`, no de quien lo construye.**

```kotlin
class CharStream(reader: Reader) {
    private val reader = reader.buffered()   // ANTES de current/next
    private var current: Int = this.reader.read()
    private var next: Int = this.reader.read()
```

El orden de declaración importa: los inicializadores de propiedades corren en orden, así que
si `current` quedara arriba leería del `Reader` crudo. Cualquier `Reader` alcanza ahora para
armar un `CharStream` eficiente.

**No se lee por líneas.** `readLine()` parece una simplificación razonable y no lo es: una
línea puede ser arbitrariamente larga, así que volvería a materializar una porción no
acotada del fuente.

**La laziness se verifica, no se documenta.** Tres tests son la evidencia:

| Test | Qué prueba |
| --- | --- |
| `lexer` — `LexerStreamingTest` | Un `Reader` que cuenta caracteres: pedir **un** token lee menos del 10% del fuente. Y construir un `CharStream` pide más de 2 caracteres, o sea que bufferiza. |
| `parser` — `ParserStreamingTest` | N statements válidos seguidos de basura léxica: los N `ParseResult.Success` salen **antes** del error. |
| `parser` — `HeapBoundedStreamingTest` | Un fuente de ~20 MB (generado en `@TempDir`, nunca commiteado) se lexea y parsea completo con `maxHeapSize = "64m"`. |

El último corre en su propia task (`:parser:heapBoundedTest`, enganchada a `check`) para no
imponerle el límite de memoria al resto de la suite. El pipeline que ejercita es
`lexer` + `parser`: `parser` no depende de `semantic` ni de `interpreter`, así que es la
cadena más larga que se puede probar sin subir a `cli`.

## Consecuencias

**Lo que se gana:** el tamaño del fuente deja de ser un límite del sistema, y la propiedad
que lo garantiza tiene tests que fallan si alguien la rompe. El test de heap acotado no es
decorativo: la variante que hace `readText()` sobre el mismo archivo muere con
`OutOfMemoryError` bajo esos mismos 64 MB.

**Lo que se resigna — el error tardío.** Con un pipeline lazy, un error que está al final del
archivo aparece cuando ya se emitió output. En `Execution`, los `println` de los statements
1..N-1 ya salieron por consola cuando el statement N falla. Esto **no se puede evitar sin
materializar**: saber que un archivo no tiene errores exige leerlo entero, y leerlo entero
antes de empezar a ejecutar es exactamente lo que la consigna prohíbe. No es un bug del
diseño, es el precio del diseño.

**El mitigante es la operación `Validation`.** El CLI ya expone un modo que recorre el mismo
pipeline (`lexer` → `parser` → `semantic`) **sin efectos**: no imprime nada del programa,
solo reporta el primer error con su posición. Quien necesita la garantía de "este archivo
está bien" antes de ejecutar corre `Validation` y después `Execution`. Cuesta una pasada
extra sobre el archivo, y esa pasada es el precio de no tener el archivo en memoria.

**Alternativas descartadas:**

- *Materializar y validar todo antes de ejecutar.* Elimina el error tardío, incumple la
  consigna. Es lo que `Validation` hace en dos pasadas sin pagar la memoria.
- *Bufferizar en el llamador* (que `cli` pase un `BufferedReader`). Anda hasta que alguien
  construya un `CharStream` sin acordarse. El buffering dejaría de ser una propiedad del
  componente para ser una convención, y las convenciones que nadie hace cumplir se pudren.
- *Leer por líneas.* Más simple de escribir, y falso: una línea puede no caber en memoria.

**Lo que sigue abierto:** estos tests cubren `lexer` y `parser`. El leak vivo hoy está en
`cli/Main.kt` (`File(...).readText()` y el `.toList()` de `parseToAST`), que materializa el
fuente y la lista completa de statements antes de que el pipeline lazy pueda hacer su
trabajo. Está fuera del alcance de este ADR y es criterio de aceptación de su propio ticket.
Hasta que se resuelva, la evidencia de streaming existe a nivel módulo pero el CLI no la
aprovecha.
