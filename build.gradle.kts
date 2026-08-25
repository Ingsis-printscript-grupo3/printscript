//para instalar los hooks versionados de .githooks
//./gradlew installGitHooks
tasks.register<Exec>("installGitHooks") {
    group = "setup"
    description = "Apunta git a la carpeta .githooks, versionada en el repo"
    commandLine("git", "config", "core.hooksPath", ".githooks")
}
