package ru.todoapp.register

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import ru.todoapp.database.categories.Categories
import ru.todoapp.database.categories.CategoryCreateDTO
import ru.todoapp.database.tokens.TokenDTO
import ru.todoapp.database.tokens.Tokens
import ru.todoapp.database.users.UserDTO
import ru.todoapp.database.users.Users
import ru.todoapp.utils.isValidEmail
import java.time.LocalDateTime
import java.util.*
import ru.todoapp.categories.ApiResponse
import org.jetbrains.exposed.sql.transactions.transaction

class RegisterController(val call: ApplicationCall) {
    suspend fun registerNewUser() {
        try {
            val registerReceiveRemote = call.receive<RegisterReceiveRemote>()
            
            if (!registerReceiveRemote.email.isValidEmail()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Email не действителен"))
                return
            }
            
            // Проверяем существование пользователя с таким логином вне транзакции
            val userByLogin = Users.fetchUser(registerReceiveRemote.login)
            if (userByLogin != null) {
                call.respond(HttpStatusCode.Conflict, ApiResponse(success = false, message = "Пользователь с таким логином уже существует"))
                return
            }
            
            // Используем один метод в модели Users, который создаст пользователя и вернет его ID
            // Это решит проблему с нарушением внешнего ключа
            val userId = transaction {
                val user = UserDTO(
                    id = UUID.randomUUID().toString(),
                    login = registerReceiveRemote.login,
                    password = registerReceiveRemote.password,
                    email = registerReceiveRemote.email,
                    username = "",
                    createdAt = LocalDateTime.now()
                )
                
                Users.insert(user)
            }
            
            // Проверяем, что пользователь действительно создан
            val createdUser = Users.fetchUserById(userId)
            if (createdUser == null) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = "Не удалось создать пользователя"))
                return
            }
            
            // Создаем токен только после подтверждения существования пользователя
            val token = UUID.randomUUID().toString()
            transaction {
                Tokens.insert(
                    TokenDTO(
                        rowId = UUID.randomUUID().toString(),
                        login = registerReceiveRemote.login,
                        userId = userId,
                        token = token
                    )
                )
            }
            
            // Создаем стандартные категории
            transaction {
                val defaultCategories = listOf("ДОМ", "РАБОТА", "УЧЁБА", "ДРУГОЕ")
                defaultCategories.forEach { categoryName ->
                    Categories.insert(
                        CategoryCreateDTO(
                            name = categoryName,
                            isDefault = true,
                            userId = userId
                        )
                    )
                }
            }
            
            // Успешная регистрация
            call.respond(HttpStatusCode.Created, RegisterResponseRemote(token = token))
            
        } catch (e: Exception) {
            println("Ошибка при регистрации: ${e.message}")
            e.printStackTrace()
            
            // Если ошибка связана с дубликатом, выдаем более конкретное сообщение
            val errorMessage = e.message?.lowercase() ?: ""
            if (errorMessage.contains("duplicate") || errorMessage.contains("unique constraint")) {
                call.respond(HttpStatusCode.Conflict, ApiResponse(success = false, message = "Пользователь с такими данными уже существует"))
            } else {
                // Общая ошибка сервера
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = "Ошибка при регистрации пользователя: ${e.message}"))
            }
        }
    }
} 