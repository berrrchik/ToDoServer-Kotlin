package ru.todoapp.database.tasks

import java.time.LocalDateTime

enum class TaskPriority {
    НИЗКИЙ, СРЕДНИЙ, ВЫСОКИЙ
}

data class TaskDTO(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
    val priority: TaskPriority,
    val categoryId: String,
    val userId: String,
    val deadline: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class TaskCreateDTO(
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val categoryId: String,
    val userId: String,
    val deadline: LocalDateTime?
) 