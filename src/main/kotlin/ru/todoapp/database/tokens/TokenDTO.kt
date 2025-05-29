package ru.todoapp.database.tokens

import java.time.LocalDateTime

data class TokenDTO(
    val rowId: String,
    val login: String,
    val userId: String,
    val token: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
) 