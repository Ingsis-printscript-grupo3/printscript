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

tasks.test {
    useJUnitPlatform { excludeTags("load") }
}

tasks.register<Test>("loadTest") {
    group = "verification"
    description = "Runs the streaming load test with a small heap"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("load") }
    maxHeapSize = "16m"
}
