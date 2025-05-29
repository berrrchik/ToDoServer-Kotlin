package ru.todoapp.login

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import ru.todoapp.database.tokens.TokenDTO
import ru.todoapp.database.tokens.Tokens
import ru.todoapp.database.users.Users
import java.util.*

class LoginController(private val call: ApplicationCall) {
    suspend fun performLogin() {
        val receive = call.receive<LoginReceiveRemote>()
        val userDTO = Users.fetchUser(receive.login)
        
        if (userDTO == null) {
            call.respond(HttpStatusCode.BadRequest, "Пользователь не найден")
            return
        }
        
        if (userDTO.password == receive.password) {
            val token = UUID.randomUUID().toString()
            Tokens.insert(
                TokenDTO(
                    rowId = UUID.randomUUID().toString(),
                    login = receive.login,
                    userId = userDTO.id,
                    token = token
                )
            )
            call.respond(LoginResponseRemote(token = token))
        } else {
            call.respond(HttpStatusCode.BadRequest, "Неверный пароль")
        }
    }
} 