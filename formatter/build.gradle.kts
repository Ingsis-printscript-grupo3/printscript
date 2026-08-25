import org.gradle.kotlin.dsl.jacoco

plugins {
    id("printscript.kotlin-common-conventions")
    jacoco
}

dependencies{
    implementation(project(":ast"))
    implementation(project(":common"))
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit { minimum = "0.80".toBigDecimal() }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}