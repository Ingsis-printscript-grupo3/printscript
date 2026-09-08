//registra la tarea installGitHooks, q aparece al correr ./gradlew tasks
//cada uno la corre una sola vez
tasks.register<Exec>("installGitHooks") {
    group = "setup"
    description = "Points git to the versioned .githooks folder so the pre-commit and pre-push hooks run"
    commandLine("git", "config", "core.hooksPath", ".githooks")
}
