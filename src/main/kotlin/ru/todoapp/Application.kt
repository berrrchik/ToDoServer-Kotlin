package ru.todoapp

import ru.todoapp.database.users.Users
import ru.todoapp.database.tokens.Tokens
import ru.todoapp.database.tasks.Tasks
import ru.todoapp.database.categories.Categories
import ru.todoapp.database.hidden_categories.HiddenCategories
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.Database
import ru.todoapp.login.configureLoginRouting
import ru.todoapp.register.configureRegisterRouting
import ru.todoapp.tasks.configureTasksRouting
import ru.todoapp.categories.configureCategoriesRouting
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.ktor.server.http.content.*
import io.ktor.server.config.*
import java.io.File

fun main(args: Array<String>) {
    // Запуск сервера с настройками из application.conf
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    // Получение настроек из конфигурационного файла
    val dbConfig = environment.config.config("database")
    val dbUrl = dbConfig.property("url").getString()
    val dbDriver = dbConfig.property("driver").getString()
    val dbUser = dbConfig.property("user").getString()
    val dbPassword = dbConfig.property("password").getString()
    
    // Подключение к базе данных с использованием настроек из конфигурации
    Database.connect(
        url = dbUrl,
        driver = dbDriver,
        user = dbUser,
        password = dbPassword
    )
    
    log.info("Подключение к базе данных: $dbUrl (пользователь: $dbUser)")

    transaction {
        SchemaUtils.create(Users)
        SchemaUtils.create(Tokens)
        SchemaUtils.create(Categories)
        SchemaUtils.create(Tasks)
        SchemaUtils.create(HiddenCategories)
        
        Categories.fixDefaultCategories()
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        anyHost()
    }

    install(ContentNegotiation) {
        json(kotlinx.serialization.json.Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    routing {
        get("/") {
            call.respond("TodoApp API работает")
        }
        
        staticResources("/swagger", "openapi") {
            default("index.html")
        }
        
        get("/openapi.yaml") {
            val yamlFile = javaClass.classLoader.getResource("openapi/documentation.yaml")?.let { 
                File(it.file)
            }
            if (yamlFile != null && yamlFile.exists()) {
                call.respondFile(yamlFile)
            } else {
                call.respond(HttpStatusCode.NotFound, "OpenAPI спецификация не найдена")
            }
        }
    }

    configureLoginRouting()
    configureRegisterRouting()
    configureTasksRouting()
    configureCategoriesRouting()
} 