# PrintScript

Intérprete de PrintScript escrito en Kotlin, con soporte para las versiones **1.0** y **1.1**
del lenguaje. Además de ejecutar código incluye un formateador y un analizador estático
(linter), todo expuesto por un CLI.

El proyecto es un build multi-módulo de Gradle con 10 módulos.

## Diagramas

| Diagrama | Archivo |
|---|---|
| Componentes: los 10 módulos y sus dependencias | [`docs/componentes.puml`](docs/componentes.puml) |
| Secuencia: cómo interactúan durante un `execute` | [`docs/secuencia-ejecucion.puml`](docs/secuencia-ejecucion.puml) |
| Clases del lexer | [`docs/clases-lexer.puml`](docs/clases-lexer.puml) |
| Flujo del intérprete | [`docs/flujo-interprete.puml`](docs/flujo-interprete.puml) |

Están en PlantUML. Se ven con el plugin de PlantUML de IntelliJ, o pegando el contenido en
[plantuml.com](https://www.plantuml.com/plantuml/uml/).

## Módulos

Los primeros seis forman el pipeline, en el orden en que procesan el código.

**`common`** — Los tipos que todos los demás necesitan compartir: `Token`, `TokenType`,
`Position`, `LanguageVersion` y la interfaz `ScriptError`. También vive acá `ConfigParser`,
el parser de configuración JSON/YAML plano que comparten el formatter y el linter. No
depende de ningún otro módulo.

**`lexer`** — Convierte el texto fuente en tokens, de a uno por vez. No conoce la gramática:
solo sabe reconocer palabras, números, strings y símbolos. Su interfaz pública es
`LexerInterface.tokenize(): Iterator<Token>`. Se extiende agregando implementaciones de
`TokenReader` a la lista que recibe el `Lexer` por constructor.

**`ast`** — Los nodos del árbol sintáctico (`VariableDeclaration`, `PrintCall`,
`BinaryExpression`, etc.) y el `Registry<N, C, R>` genérico que usa el intérprete para
resolver qué handler atiende cada nodo. Es solo estructura: no tiene lógica de negocio.

**`parser`** — Arma el árbol a partir de los tokens. Su interfaz es
`ParserInterface.parse(): Iterator<ParseResult>`, donde cada resultado es un `Success` con
una sentencia o un `Failure` con mensaje y posición. Se extiende registrando *parselets*
(expresiones) y *handlers* (sentencias).

**`semantic`** — Valida el árbol: que las variables estén declaradas antes de usarse, que
los tipos cierren, que no se reasigne un `const`. Devuelve `Iterator<SemanticResult<Statement>>`,
sin ejecutar nada.

**`interpreter`** — Ejecuta el árbol ya validado. Mantiene el `Environment` con los scopes y
resuelve cada nodo con dos `Registry`: uno de sentencias y otro de expresiones. La salida de
`println` va por la interfaz `Output`, y la entrada de `readInput` por `InputProvider`, así
que en los tests se reemplazan sin tocar el intérprete.

Los dos siguientes son herramientas que trabajan sobre el mismo código pero no lo ejecutan.

**`formatter`** — Reformatea el código según reglas de configuración (espaciado, saltos de
línea, indentación). Las reglas son objetos compuestos en un árbol, así que agregar una no
implica tocar el formateador.

**`linter`** — Reporta violaciones de estilo y malas prácticas sin modificar el código:
formato de identificadores y restricciones sobre los argumentos de `println` y `readInput`.
Las reglas también se componen, y la configuración decide cuáles se crean.

Y los dos últimos son los que arman todo y lo exponen.

**`runner`** — El único módulo que conoce a todos los demás. Su `Engine` encadena el
pipeline completo y traduce cualquier excepción en un resultado con tipo, mensaje y
posición. Expone también `PrintScriptRunner`, que es la fachada que consume el TCK de la
cátedra.

**`cli`** — La interfaz de línea de comandos, construida con Clikt. Parsea los argumentos y
delega todo en `runner`.

## Cómo interactúan

Un `execute` recorre los módulos así:

```
CLI → Engine → Lexer → Parser → SemanticAnalyzer → Interpreter → Output
        Iterator<Token>  Iterator<Statement>  Iterator<Statement>
                                              (ya validadas)
```

Lo importante es que **cada flecha es un `Iterator`, no una lista**. Cuando el `Engine` arma
el pipeline todavía no se leyó un solo carácter del archivo: cada capa envuelve a la
anterior en un iterador perezoso. Recién cuando el intérprete pide la primera sentencia el
pedido viaja hacia atrás — una sentencia validada pide una sentencia parseada, que pide los
tokens que necesite, que piden los caracteres que necesite.

Por eso el intérprete puede procesar un archivo de varios MB sin que entre entero en
memoria, que es lo que exige el test de archivo grande del TCK.

Los módulos del pipeline **no se conocen entre sí**. El lexer no sabe que existe un parser;
el parser no sabe que existe un intérprete. Se comunican a través de los tipos de `common` y
`ast`, y es `runner` quien los conecta. Eso es lo que permite que el formatter y el linter
reusen el lexer y el parser sin arrastrar al intérprete, y que el CLI no dependa de ninguno
de los tres: entra por `runner`.

Los errores siguen el mismo camino de vuelta. El lexer y el intérprete lanzan excepciones
(`LexicalError`, `InterpreterError`); el parser y el semantic devuelven `Failure` en su
resultado. El `Engine` unifica las dos formas en un `ExecutionResult.Failure` con tipo,
mensaje y posición, para que el CLI nunca le muestre un stacktrace al usuario.

Ver [`docs/secuencia-ejecucion.puml`](docs/secuencia-ejecucion.puml) para el detalle.

## Cómo correr el CLI

El CLI tiene cuatro subcomandos. Todos aceptan `--version` (`1.0` o `1.1`, por defecto
`1.0`) y `--quiet` (no imprimir el progreso de parseo).

```bash
./gradlew :cli:run --args="execute archivo.prs --version 1.1"
```

| Subcomando | Qué hace |
|---|---|
| `execute <archivo>` | Ejecuta el archivo |
| `validate <archivo>` | Revisa errores léxicos, sintácticos y semánticos sin ejecutar |
| `format <archivo>` | Formatea e imprime el resultado. Acepta `--config` |
| `analyze <archivo>` | Analiza estilo y malas prácticas. Acepta `--config` |

El `--config` de `format` y `analyze` recibe la ruta de un archivo JSON o YAML con las
reglas:

```bash
./gradlew :cli:run --args="format archivo.prs --config reglas.json"
./gradlew :cli:run --args="analyze archivo.prs --config reglas.yaml --version 1.1"
```

Sin `--config`, el formatter aplica las reglas que la consigna pide siempre y el linter
activa todas sus reglas con los valores por defecto.

Para armar un ejecutable en vez de pasar por Gradle:

```bash
./gradlew :cli:installDist
```

## Cómo publicar

Los 10 módulos se publican como artefactos Maven bajo el grupo `org.printscript`.

**A un repositorio local**, que es lo que se usa para probar contra el TCK:

```bash
./gradlew publishToMavenLocal
```

Deja los `.jar` en `~/.m2/repository/org/printscript/`, donde cualquier proyecto Gradle o
Maven de la misma máquina los encuentra sin credenciales.

**A GitHub Packages**, que es la publicación real. La hace sola el workflow
[`.github/workflows/publish.yml`](.github/workflows/publish.yml) cuando se crea un release en
GitHub. El workflow corre `./gradlew publish` tomando la versión del tag: un release
etiquetado `v1.0.0` se publica como `1.0.0`.

Para publicar a mano hacen falta las variables de entorno `GITHUB_ACTOR` y `GITHUB_TOKEN`
con un token que tenga permiso `write:packages`:

```bash
./gradlew publish -Pversion=1.0.0
```
