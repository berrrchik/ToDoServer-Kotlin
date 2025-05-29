package ru.todoapp.tasks

import kotlinx.serialization.Serializable
import ru.todoapp.database.tasks.TaskPriority
import java.time.LocalDateTime

@Serializable
data class TaskReceiveRemote(
    val title: String,
    val description: String,
    val priority: String,
    val categoryId: String,
    val deadline: String? = null
)

@Serializable
data class TaskResponseRemote(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
    val priority: String,
    val categoryId: String,
    val deadline: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TaskUpdateRemote(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val isCompleted: Boolean? = null,
    val isDeleted: Boolean? = null,
    val priority: String? = null,
    val categoryId: String? = null,
    val deadline: String? = null
)

@Serializable
data class TaskListResponseRemote(
    val tasks: List<TaskResponseRemote>
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String
) 