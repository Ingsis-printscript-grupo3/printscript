plugins {
    id("printscript.kotlin-common-conventions")
    application
}

dependencies {
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":semantic"))
    api(project(":interpreter"))
    api(project(":ast"))
    api(project(":common"))
    implementation(project(":formatter"))
    api(project(":linter"))
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
}

application {
    mainClass.set("printscript.cli.MainKt")
}
