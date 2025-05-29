package ru.todoapp.database.categories

import java.time.LocalDateTime

data class CategoryDTO(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val isHidden: Boolean,
    val userId: String,
    val createdAt: LocalDateTime
)

data class CategoryCreateDTO(
    val name: String,
    val isDefault: Boolean,
    val userId: String
) 