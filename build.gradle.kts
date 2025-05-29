plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    application
}

group = "ru.todoapp"
version = "0.0.1"

application {
    mainClass = "ru.todoapp.ApplicationKt"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cio)
    implementation(libs.logback.classic)
    implementation("io.ktor:ktor-server-cors:2.3.4")
    implementation("io.ktor:ktor-server-config-yaml:2.3.4")
    implementation("io.ktor:ktor-server-host-common:2.3.4")
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.exposed:exposed-java-time:0.40.1")
    implementation("org.flywaydb:flyway-core:9.17.0")
    
    // Swagger/OpenAPI зависимости
    implementation("io.ktor:ktor-server-swagger:2.3.4")
    implementation("io.swagger.core.v3:swagger-core:2.2.15")
    implementation("io.swagger.core.v3:swagger-models:2.2.15")
    implementation("io.swagger.core.v3:swagger-integration:2.2.15")
    implementation("org.webjars:swagger-ui:5.1.0")
}

// Настройка для создания исполняемого fat JAR
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "io.ktor.server.cio.EngineMain"
    }
    
    // Включение всех зависимостей в JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Test> {
    // Пропускать тесты при сборке, если нужно
    // enabled = false
}