# PrintScript

Intérprete de PrintScript escrito en Kotlin, con soporte para las versiones **1.0** y **1.1**
del lenguaje. Además de ejecutar código incluye un formateador y un analizador estático
(linter), todo expuesto por un CLI.

El proyecto es un build multi-módulo de Gradle con 10 módulos.

## Requisitos

- **JDK 21.** Los módulos fijan el toolchain en 21 y `gradle.properties` tiene
  `org.gradle.java.installations.auto-download=false`, así que Gradle no lo descarga solo:
  tiene que estar instalado.
- No hace falta instalar Gradle: el repo trae el wrapper (`./gradlew`).

Después de clonar, conviene instalar los hooks de git una vez. Corren ktlint antes de cada
commit y el `check` completo antes de cada push:

```bash
./gradlew installGitHooks
```

Para compilar y correr toda la verificación (tests, ktlint, detekt, cobertura mínima del 80%
por módulo y el test de carga con heap acotado):

```bash
./gradlew check
```

## Diagramas

| Diagrama | Archivo |
|---|---|
| Componentes: los 10 módulos y sus dependencias | [`docs/componentes.puml`](docs/componentes.puml) |
| Secuencia: cómo interactúan durante un `execute` | [`docs/secuencia-ejecucion.puml`](docs/secuencia-ejecucion.puml) |
| Clases del lexer | [`docs/clases-lexer.puml`](docs/clases-lexer.puml) |
| Clases del Registry (extensibilidad de semantic e interpreter) | [`docs/clases-registry.puml`](docs/clases-registry.puml) |
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
`BinaryExpression`, etc.) y el `Registry<N, C, R>` genérico que resuelve qué handler atiende
cada nodo. Es solo estructura: no tiene lógica de negocio.

**`parser`** — Arma el árbol a partir de los tokens. Su interfaz es
`ParserInterface.parse(): Iterator<ParseResult>`, donde cada resultado es un `Success` con
una sentencia o un `Failure` con mensaje y posición. Se extiende registrando *parselets*
(expresiones) y *handlers* (sentencias).

**`semantic`** — Valida el árbol: que las variables estén declaradas antes de usarse, que
los tipos cierren, que no se reasigne un `const`. Usa el `Registry` de `ast` en
`StatementValidator` y `ExpressionResolver`. Devuelve `Iterator<SemanticResult<Statement>>`,
sin ejecutar nada.

**`interpreter`** — Ejecuta el árbol ya validado. Mantiene el `Environment` con los scopes y
resuelve cada nodo con dos `Registry` de `ast`: uno de sentencias y otro de expresiones. La
salida de `println` va por la interfaz `Output`, y la entrada de `readInput` por
`InputProvider`, así que en los tests se reemplazan sin tocar el intérprete.

Los dos siguientes son herramientas que trabajan sobre el mismo código pero no lo ejecutan.

**`formatter`** — Reformatea el código según reglas de configuración (espaciado, saltos de
línea, indentación). Las reglas son objetos compuestos en un árbol, así que agregar una no
implica tocar el formateador.

**`linter`** — Reporta violaciones de estilo y malas prácticas sin modificar el código:
formato de identificadores y restricciones sobre los argumentos de `println` y `readInput`.
Las reglas también se componen, y la configuración decide cuáles se crean.

Y los dos últimos son los que arman todo y lo exponen.

**`runner`** — El único módulo que depende del pipeline completo. Su `Engine` lo encadena y
traduce cualquier excepción en un resultado con tipo, mensaje y posición. Expone también
`PrintScriptRunner`, que es la fachada que consume el TCK de la cátedra.

**`cli`** — La interfaz de línea de comandos, construida con Clikt. Delega la ejecución en
`runner`; de `interpreter`, `formatter` y `linter` toma solo los tipos que necesita para
armar las opciones (`ConsoleOutput`, las reglas de formato y de lint).

## Cómo interactúan

Un `execute` recorre los módulos así:

```
CLI → Engine → Lexer → Parser → SemanticAnalyzer → Interpreter → Output
        Iterator<Token>  Iterator<Statement>  Iterator<Statement>
                                              (ya validadas)
```

Lo importante es que **cada flecha es un `Iterator`, no una lista**. Cuando el `Engine`
termina de armar el pipeline casi no se leyó nada del archivo: solo el primer token, porque
el `TokenStream` lo pide al construirse y el `CharStream` arranca con dos caracteres
adelantados. De ahí en más, el intérprete tira del iterador y cada pedido viaja hacia atrás
— una sentencia validada pide una sentencia parseada, que pide los tokens que necesite, que
piden los caracteres que necesite.

Por eso el intérprete puede procesar un archivo de varios MB sin que entre entero en
memoria, que es lo que exige el test de archivo grande del TCK.

Los módulos del pipeline **no se conocen entre sí**. El lexer no sabe que existe un parser;
el parser no sabe que existe un intérprete. Se comunican a través de los tipos de `common` y
`ast`, y es `runner` quien los conecta. Eso es lo que permite que el formatter y el linter
reusen el lexer y el parser sin arrastrar al intérprete, y que el CLI no dependa de `lexer`,
`parser` ni `semantic`: llega a ellos a través de `runner`.

Los errores siguen el mismo camino de vuelta. El lexer y el intérprete lanzan excepciones
(`LexicalError`, `InterpreterError`); el parser y el semantic devuelven `Failure` en su
resultado. El `Engine` unifica las dos formas en un `ExecutionResult.Failure` con tipo,
mensaje y posición, para que el CLI nunca le muestre un stacktrace al usuario.

Ver [`docs/secuencia-ejecucion.puml`](docs/secuencia-ejecucion.puml) para el detalle.

## Cómo correr el CLI

Primero armar el ejecutable:

```bash
./gradlew :cli:installDist
```

Eso deja el binario en `cli/build/install/cli/bin/cli` (y `cli.bat` en Windows). Se corre
desde la raíz del repo:

```bash
./cli/build/install/cli/bin/cli execute archivo.prs --version 1.1
```

> **Usar el binario y no `./gradlew :cli:run`.** La tarea `run` de Gradle corre con el
> directorio de trabajo en `cli/`, así que las rutas relativas no se encuentran, y no le
> pasa la entrada estándar al proceso, así que `readInput` no funciona.

El CLI tiene cuatro subcomandos. Todos aceptan `--version` (`1.0` o `1.1`, por defecto
`1.0`) y `--quiet` (no imprimir el progreso de parseo).

| Subcomando | Qué hace |
|---|---|
| `execute <archivo>` | Ejecuta el archivo |
| `validate <archivo>` | Revisa errores léxicos, sintácticos y semánticos sin ejecutar |
| `format <archivo>` | Formatea e imprime el resultado. Acepta `--config` |
| `analyze <archivo>` | Analiza estilo y malas prácticas. Acepta `--config` |

El `--config` de `format` y `analyze` recibe la ruta de un archivo JSON o YAML con las
reglas:

```bash
./cli/build/install/cli/bin/cli format archivo.prs --config reglas.json
./cli/build/install/cli/bin/cli analyze archivo.prs --config reglas.yaml --version 1.1
```

Sin `--config`, el formatter aplica las reglas que la consigna pide siempre y el linter
activa todas sus reglas con los valores por defecto.

## Cómo publicar

Los 10 módulos se publican como artefactos Maven bajo el grupo `org.printscript`. La versión
por defecto es la de `gradle.properties` (`0.0.1-SNAPSHOT`) y se pisa con `-Pversion`.

**A un repositorio local**, que es lo que se usa para probar contra el TCK:

```bash
./gradlew publishToMavenLocal "-Pversion=1.0.0"
```

> Las comillas alrededor de `-Pversion=1.0.0` son necesarias en PowerShell: sin ellas,
> interpreta el `=` como separador de argumentos y trunca el valor (`Task '.0.0' not found`).
> En bash no hacen falta, pero tampoco molestan, así que quedan puestas en todos los
> ejemplos de este documento.

> **Para probar contra el TCK, publicá sin `-Pversion`.** El `build.gradle` del fork
> (rama `group-3-validation`) pide `org.printscript:runner:0.0.1-SNAPSHOT`, que es la versión
> por defecto de `gradle.properties`. Si publicás con `-Pversion=1.0.0`, el TCK no usa lo que
> acabás de publicar: sigue resolviendo el `0.0.1-SNAPSHOT` viejo que haya en `~/.m2` y
> testea código antiguo sin avisar.

Los `.jar` quedan en `~/.m2/repository/org/printscript/`, donde cualquier proyecto Gradle o
Maven de la misma máquina los encuentra sin credenciales.

**A GitHub Packages**, que es la publicación real. La hace sola el workflow
[`.github/workflows/publish.yml`](.github/workflows/publish.yml) cuando se crea un release en
GitHub. El workflow corre `./gradlew publish` tomando la versión del tag: un release
etiquetado `v1.0.0` se publica como `1.0.0`.

Para publicar a mano hacen falta las variables de entorno `GITHUB_ACTOR` y `GITHUB_TOKEN`
con un token que tenga permiso `write:packages`:

```bash
./gradlew publish "-Pversion=1.0.1"
```

> GitHub Packages **no permite subir dos veces la misma versión**. La `1.0.0` ya está
> publicada, así que una republicación tiene que ir con un número nuevo.

## Cómo correr el TCK

El TCK de la cátedra vive en un repo aparte y se engancha a esta implementación por
`CustomImplementationFactory`, que adapta `PrintScriptRunner` a sus interfaces.

```bash
# 1. publicar esta implementación en el repositorio local, con la version por defecto
#    (0.0.1-SNAPSHOT), que es la que pide el build.gradle del fork
./gradlew publishToMavenLocal

# 2. clonar el fork del grupo, en la rama de validación
git clone -b group-3-validation https://github.com/Ingsis-printscript-grupo3/printscript-tck.git

# 3. correr la suite
cd printscript-tck && ./gradlew build
```

El `build.gradle` del TCK declara `mavenLocal()` antes que GitHub Packages, así que resuelve
el artefacto recién publicado sin pedir credenciales.

> Si el TCK falla de forma rara —tests que fallan con código que local pasa— lo primero a
> revisar es la fecha de los `.jar` en `~/.m2/repository/org/printscript/0.0.1-SNAPSHOT/`.
> Si no son de recién, el TCK está probando otra cosa.
