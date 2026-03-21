plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.ua.astrumon"
version = "0.1.2"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Tests
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.h2)

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
    implementation(libs.exposed.migration)

    // Database drivers
    implementation(libs.sqlite.jdbc)
    implementation(libs.postgresql)

    // Connection pool
    implementation(libs.hikari)

    // Db Migration
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

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

application {
    mainClass.set("com.ua.astrumon.MainKt")
}

registerAppTasks()

tasks.test {
    useJUnitPlatform()
}