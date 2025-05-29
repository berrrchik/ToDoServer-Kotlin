package ru.todoapp.tasks

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import ru.todoapp.database.tasks.TaskCreateDTO
import ru.todoapp.database.tasks.TaskDTO
import ru.todoapp.database.tasks.TaskPriority
import ru.todoapp.database.tasks.Tasks
import ru.todoapp.database.tokens.Tokens
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TasksController(private val call: ApplicationCall) {
    
    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    suspend fun createTask() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val receiveTask = call.receive<TaskReceiveRemote>()
        
        try {
            val taskCreateDTO = TaskCreateDTO(
                title = receiveTask.title,
                description = receiveTask.description,
                priority = TaskPriority.valueOf(receiveTask.priority),
                categoryId = receiveTask.categoryId,
                userId = tokenDTO.userId,
                deadline = receiveTask.deadline?.let { LocalDateTime.parse(it, dateFormatter) }
            )
            
            val createdTask = Tasks.insert(taskCreateDTO)
            
            call.respond(HttpStatusCode.Created, mapTaskToResponse(createdTask))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Не удалось создать задачу: ${e.message}")
        }
    }
    
    suspend fun getTasks() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val tasks = Tasks.fetchTasksByUser(tokenDTO.userId)
        
        call.respond(TaskListResponseRemote(
            tasks = tasks.map { mapTaskToResponse(it) }
        ))
    }
    
    suspend fun getTaskById() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val taskId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID задачи не указан")
        
        val task = Tasks.fetchTaskById(taskId) ?: return call.respond(HttpStatusCode.NotFound, "Задача не найдена")
        
        if (task.userId != tokenDTO.userId) {
            return call.respond(HttpStatusCode.Forbidden, "Доступ запрещен")
        }
        
        call.respond(mapTaskToResponse(task))
    }
    
    suspend fun updateTask() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        println("updateTask: НАЧАЛО ДИАГНОСТИКИ =======================")
        
        val updateTask = call.receive<TaskUpdateRemote>()
        val taskId = updateTask.id
        
        println("updateTask: ID задачи: '$taskId'")
        println("updateTask: Получены поля для обновления:")
        println("updateTask: title = ${updateTask.title}")
        println("updateTask: description = ${updateTask.description}")
        println("updateTask: isCompleted = ${updateTask.isCompleted}")
        println("updateTask: isDeleted = ${updateTask.isDeleted}")
        println("updateTask: priority = ${updateTask.priority}")
        println("updateTask: categoryId = ${updateTask.categoryId}")
        println("updateTask: deadline = ${updateTask.deadline}")
        
        val task = Tasks.fetchTaskById(taskId) ?: return call.respond(HttpStatusCode.NotFound, "Задача не найдена")
        
        if (task.userId != tokenDTO.userId) {
            println("updateTask: Доступ запрещен - ID пользователя не совпадает")
            return call.respond(HttpStatusCode.Forbidden, "Доступ запрещен")
        }
        
        try {
            val updatedTask = TaskDTO(
                id = task.id,
                title = updateTask.title ?: task.title,
                description = updateTask.description ?: task.description,
                isCompleted = updateTask.isCompleted ?: task.isCompleted,
                isDeleted = updateTask.isDeleted ?: task.isDeleted,
                priority = updateTask.priority?.let { TaskPriority.valueOf(it) } ?: task.priority,
                categoryId = updateTask.categoryId ?: task.categoryId,
                userId = task.userId,
                deadline = updateTask.deadline?.let { LocalDateTime.parse(it, dateFormatter) } ?: task.deadline,
                createdAt = task.createdAt,
                updatedAt = LocalDateTime.now()
            )
            
            println("updateTask: Задача после обновления:")
            println("updateTask: title = ${updatedTask.title}")
            println("updateTask: description = ${updatedTask.description}")
            println("updateTask: isCompleted = ${updatedTask.isCompleted}")
            println("updateTask: isDeleted = ${updatedTask.isDeleted}")
            println("updateTask: priority = ${updatedTask.priority}")
            println("updateTask: categoryId = ${updatedTask.categoryId}")
            
            val success = Tasks.updateTask(updatedTask)
            
            if (success) {
                println("updateTask: Задача успешно обновлена")
                call.respond(mapTaskToResponse(updatedTask))
            } else {
                println("updateTask: Ошибка при обновлении задачи")
                call.respond(HttpStatusCode.InternalServerError, "Не удалось обновить задачу")
            }
        } catch (e: Exception) {
            println("updateTask: Исключение при обновлении задачи: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.BadRequest, "Не удалось обновить задачу: ${e.message}")
        }
        
        println("updateTask: КОНЕЦ ДИАГНОСТИКИ =======================")
    }
    
    suspend fun deleteTask() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val taskId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID задачи не указан")
        
        val task = Tasks.fetchTaskById(taskId) ?: return call.respond(HttpStatusCode.NotFound, "Задача не найдена")
        
        if (task.userId != tokenDTO.userId) {
            return call.respond(HttpStatusCode.Forbidden, "Доступ запрещен")
        }
        
        val success = Tasks.deleteTask(taskId)
        
        if (success) {
            call.respond(HttpStatusCode.OK, "Задача успешно удалена")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Не удалось удалить задачу")
        }
    }
    
    /**
     * Полностью удаляет задачу из базы данных.
     * Этот метод следует использовать только для задач, которые уже помечены как удаленные (isDeleted = true)
     */
    suspend fun permanentlyDeleteTask() {
        println("permanentlyDeleteTask: Получен запрос на полное удаление задачи")
        
        try {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
            println("permanentlyDeleteTask: Токен получен: ${token.take(10)}...")
            
            val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
            println("permanentlyDeleteTask: Токен валидный, пользователь: ${tokenDTO.userId}")
            
            val taskId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID задачи не указан")
            println("permanentlyDeleteTask: ID задачи: $taskId")
            
            val task = Tasks.fetchTaskById(taskId)
            if (task == null) {
                println("permanentlyDeleteTask: Задача не найдена")
                return call.respond(HttpStatusCode.NotFound, ApiResponse(success = false, message = "Задача не найдена"))
            }
            
            if (task.userId != tokenDTO.userId) {
                println("permanentlyDeleteTask: Доступ запрещен - пользователь ${tokenDTO.userId} пытается удалить задачу пользователя ${task.userId}")
                return call.respond(HttpStatusCode.Forbidden, ApiResponse(success = false, message = "Доступ запрещен"))
            }
            
            if (!task.isDeleted) {
                println("permanentlyDeleteTask: Попытка полного удаления неудаленной задачи")
                return call.respond(HttpStatusCode.BadRequest, ApiResponse(success = false, message = "Только задачи, помеченные как удаленные, могут быть удалены полностью"))
            }
            
            println("permanentlyDeleteTask: Вызываем Tasks.permanentlyDeleteTask для ID: $taskId")
            val success = Tasks.permanentlyDeleteTask(taskId)
            
            if (success) {
                println("permanentlyDeleteTask: Задача успешно удалена из базы данных")
                try {
                    // Используем класс ApiResponse вместо Map для стабильной сериализации
                    val response = ApiResponse(success = true, message = "Задача успешно удалена из базы данных")
                    println("permanentlyDeleteTask: Отправляем ответ: $response")
                    call.respond(HttpStatusCode.OK, response)
                    println("permanentlyDeleteTask: Ответ успешно отправлен")
                } catch (e: Exception) {
                    println("permanentlyDeleteTask: Ошибка при отправке ответа: ${e.message}")
                    e.printStackTrace()
                    // Пробуем самый простой ответ
                    call.respondText("Задача успешно удалена", ContentType.Text.Plain, HttpStatusCode.OK)
                }
            } else {
                println("permanentlyDeleteTask: Не удалось удалить задачу из базы данных")
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = "Не удалось удалить задачу из базы данных"))
            }
        } catch (e: Exception) {
            println("permanentlyDeleteTask: Неожиданная ошибка: ${e.message}")
            e.printStackTrace()
            try {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = "Внутренняя ошибка сервера: ${e.message}"))
            } catch (e2: Exception) {
                println("permanentlyDeleteTask: Критическая ошибка при отправке ответа об ошибке: ${e2.message}")
                call.respondText("Внутренняя ошибка сервера", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
            }
        }
    }
    
    private fun mapTaskToResponse(task: TaskDTO): TaskResponseRemote {
        return TaskResponseRemote(
            id = task.id,
            title = task.title,
            description = task.description,
            isCompleted = task.isCompleted,
            isDeleted = task.isDeleted,
            priority = task.priority.name,
            categoryId = task.categoryId,
            deadline = task.deadline?.format(dateFormatter),
            createdAt = task.createdAt.format(dateFormatter),
            updatedAt = task.updatedAt.format(dateFormatter)
        )
    }
} 