plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.3.0"
    application
}

group = "de.scandurra"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-cio:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("de.scandurra.githubactionswatch.MainKt")
}
