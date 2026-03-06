plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))
    // Env
    implementation(libs.dotenv)

    // Koin (DI)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines + Flow
    implementation(libs.kotlinx.coroutines.core)

    // Exposed (ORM)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.date)

    // Database drivers
    implementation(libs.sqlite.jdbc)
    implementation(libs.postgresql)

    // Connection pool
    implementation(libs.hikari)

    // Logging
    implementation(libs.logback)

    // Ktor (HTTP server)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Telegram
    implementation(libs.telegram.bot)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}