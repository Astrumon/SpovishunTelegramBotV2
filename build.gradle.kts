plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.ua.astrumon"
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

application {
    mainClass.set("com.ua.astrumon.MainKt")
}

tasks.register<JavaExec>("runDev") {
    group = "application"
    description = "Run the application in development mode"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.ua.astrumon.MainKt")
    environment("PROFILE", "dev")
}

tasks.register<JavaExec>("runProd") {
    group = "application"
    description = "Run the application in production mode"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.ua.astrumon.MainKt")
    environment("PROFILE", "prod")
}

tasks.test {
    useJUnitPlatform()
}