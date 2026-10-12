plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    application
}

group = "shb2dev"
version = "1.1.0"

repositories {
    mavenCentral()
}

application {
    mainClass = "shb2dev.utilbot.MainKt"
}

dependencies {
    implementation("dev.kord:kord-core:0.18.1")
// Source: https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.6.5")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}