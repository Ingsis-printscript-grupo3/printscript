import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("jvm")
    `java-library`
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    jacoco
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
}

// detekt saca su JDK de aca
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = providers.gradleProperty("group").getOrElse("org.printscript")
version = providers.gradleProperty("version").getOrElse("0.0.1-SNAPSHOT")

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Ingsis-printscript-grupo3/printscript")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}


repositories{
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

// sin esto la verificacion no corre los tests: mide el .exec que haya quedado de la ultima
// corrida, que puede ser de antes de los cambios (o no existir, y entonces se saltea y da verde).
// Paso en la demo y hacia falta un clean check. Si los tests estan al dia, Gradle no los repite.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit { minimum = "0.80".toBigDecimal() }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
