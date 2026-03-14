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

// Generate version info from build.gradle
tasks.register("generateVersionInfo") {
    group = "build"
    description = "Generate version info file"
    
    doLast {
        val versionInfoFile = file("src/main/kotlin/common/util/VersionInfo.kt")
        val versionInfoContent = """
            package com.ua.astrumon.common.util
            
            object VersionInfo {
                private const val VERSION = "$version"
                const val BOT_NAME = "Spovishun"
                
                fun getFullVersion(): String = BOT_NAME + " v" + VERSION
            }
        """.trimIndent()
        
        versionInfoFile.writeText(versionInfoContent)
        println("Generated VersionInfo.kt with version: $version")
    }
}

// Make sure version info is generated before compile
tasks.named("compileKotlin") {
    dependsOn("generateVersionInfo")
}