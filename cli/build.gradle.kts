plugins {
    id("printscript.kotlin-common-conventions")
    application
}

dependencies {
    implementation(project(":runner"))
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
}

application {
    mainClass.set("printscript.cli.MainKt")
}
