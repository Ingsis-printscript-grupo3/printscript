plugins {
    id("printscript.kotlin-common-conventions")
    jacoco
}

dependencies {
    implementation(project(":ast"))
    implementation(project(":common"))
}

//por ahora solo el reporte, sin enganchar el umbral a check
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}
