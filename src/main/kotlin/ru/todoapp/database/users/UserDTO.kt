package ru.todoapp.database.users

import java.time.LocalDateTime

data class UserDTO(
    val id: String,
    val login: String,
    val password: String,
    val email: String,
    val username: String,
    val createdAt: LocalDateTime
) 