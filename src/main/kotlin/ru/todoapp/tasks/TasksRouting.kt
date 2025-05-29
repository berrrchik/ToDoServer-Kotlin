package ru.todoapp.tasks

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureTasksRouting() {
    routing {
        route("/tasks") {
            // Получение всех задач пользователя
            get {
                val tasksController = TasksController(call)
                tasksController.getTasks()
            }
            
            // Создание новой задачи
            post {
                val tasksController = TasksController(call)
                tasksController.createTask()
            }
            
            // Получение задачи по ID
            get("/{id}") {
                val tasksController = TasksController(call)
                tasksController.getTaskById()
            }
            
            // Обновление задачи
            put {
                val tasksController = TasksController(call)
                tasksController.updateTask()
            }
            
            // Удаление задачи (отметка как удаленной)
            delete("/{id}") {
                val tasksController = TasksController(call)
                tasksController.deleteTask()
            }
            
            // Полное удаление задачи из базы данных
            delete("/permanently/{id}") {
                val tasksController = TasksController(call)
                tasksController.permanentlyDeleteTask()
            }
        }
    }
} 