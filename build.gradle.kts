val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val dotenv_version: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "3.1.3"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "org.delcom.watchlist"
version = "1.0.0"

application {
    mainClass = "org.delcom.watchlist.ApplicationKt"
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // ── Ktor Server ───────────────────────────────────────────────────────────
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")

    // ── Serialization ─────────────────────────────────────────────────────────
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    // ── Database (Exposed + PostgreSQL) ───────────────────────────────────────
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.exposed:exposed-core:0.52.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.52.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.52.0")

    // ── Dependency Injection (Koin) ───────────────────────────────────────────
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.6")

    // ── Security ──────────────────────────────────────────────────────────────
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.auth0:java-jwt:4.4.0")

    // ── Dotenv ────────────────────────────────────────────────────────────────
    implementation("io.github.cdimascio:dotenv-kotlin:$dotenv_version")

    // ── Logging ───────────────────────────────────────────────────────────────
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // ── Test ──────────────────────────────────────────────────────────────────
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
