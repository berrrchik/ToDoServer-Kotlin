package ru.todoapp.database.hidden_categories

import java.time.LocalDateTime

data class HiddenCategoryDTO(
    val id: String,
    val userId: String,
    val categoryId: String,
    val createdAt: LocalDateTime
) 