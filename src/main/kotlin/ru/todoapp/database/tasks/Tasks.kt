package ru.todoapp.database.tasks

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import ru.todoapp.database.users.Users
import ru.todoapp.database.categories.Categories
import java.time.LocalDateTime
import java.util.*

object Tasks : Table("tasks") {
    val id = varchar("id", 50)
    val title = varchar("title", 255)
    val description = text("description")
    val isCompleted = bool("is_completed").default(false)
    val isDeleted = bool("is_deleted").default(false)
    val priority = varchar("priority", 20)
    val categoryId = varchar("category_id", 50).references(Categories.id).nullable()
    val userId = varchar("user_id", 50).references(Users.id)
    val deadline = datetime("deadline").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    fun insert(taskCreateDTO: TaskCreateDTO): TaskDTO {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        
        val task = TaskDTO(
            id = id,
            title = taskCreateDTO.title,
            description = taskCreateDTO.description,
            isCompleted = false,
            isDeleted = false,
            priority = taskCreateDTO.priority,
            categoryId = taskCreateDTO.categoryId,
            userId = taskCreateDTO.userId,
            deadline = taskCreateDTO.deadline,
            createdAt = now,
            updatedAt = now
        )
        
        transaction {
            insert {
                it[Tasks.id] = task.id
                it[title] = task.title
                it[description] = task.description
                it[isCompleted] = task.isCompleted
                it[isDeleted] = task.isDeleted
                it[priority] = task.priority.name
                it[categoryId] = task.categoryId
                it[userId] = task.userId
                it[deadline] = task.deadline
                it[createdAt] = task.createdAt
                it[updatedAt] = task.updatedAt
            }
        }
        
        return task
    }
    
    fun fetchTasksByUser(userId: String): List<TaskDTO> {
        return transaction {
            select { Tasks.userId.eq(userId) }
                .map { row ->
                    mapRowToTaskDTO(row)
                }
        }
    }
    
    fun fetchTaskById(id: String): TaskDTO? {
        return try {
            transaction {
                select { Tasks.id.eq(id) }
                    .singleOrNull()
                    ?.let { mapRowToTaskDTO(it) }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun updateTask(task: TaskDTO): Boolean {
        return try {
            transaction {
                update({ Tasks.id.eq(task.id) }) {
                    it[title] = task.title
                    it[description] = task.description
                    it[isCompleted] = task.isCompleted
                    it[isDeleted] = task.isDeleted
                    it[priority] = task.priority.name
                    it[categoryId] = task.categoryId
                    it[deadline] = task.deadline
                    it[updatedAt] = LocalDateTime.now()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteTask(id: String): Boolean {
        return try {
            transaction {
                update({ Tasks.id.eq(id) }) {
                    it[isDeleted] = true
                    it[updatedAt] = LocalDateTime.now()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Физически удаляет задачу из базы данных
     * @param id идентификатор задачи для удаления
     * @return true если удаление успешно, false в случае ошибки
     */
    fun permanentlyDeleteTask(id: String): Boolean {
        println("Tasks.permanentlyDeleteTask: Начало физического удаления задачи с ID: $id")
        
        try {
            // Сначала проверим существование задачи
            val taskExists = transaction {
                select { Tasks.id.eq(id) }
                    .count() > 0
            }
            
            if (!taskExists) {
                println("Tasks.permanentlyDeleteTask: Задача с ID: $id не найдена в БД")
                return false
            }
            
            println("Tasks.permanentlyDeleteTask: Задача найдена, начинаем удаление")
            
            val rowsDeleted = transaction {
                try {
                    // Для диагностики сначала выберем задачу
                    val task = select { Tasks.id.eq(id) }.singleOrNull()
                    if (task == null) {
                        println("Tasks.permanentlyDeleteTask: Странная ситуация - задача не найдена внутри транзакции")
                        return@transaction 0
                    }
                    
                    println("Tasks.permanentlyDeleteTask: Найдена задача для удаления: " +
                            "title=${task[title]}, " +
                            "isCompleted=${task[isCompleted]}, " +
                            "isDeleted=${task[isDeleted]}")
                    
                    // Теперь удаляем
                    deleteWhere { Tasks.id.eq(id) }
                } catch (e: Exception) {
                    println("Tasks.permanentlyDeleteTask: Ошибка внутри транзакции: ${e.message}")
                    e.printStackTrace()
                    0
                }
            }
            
            println("Tasks.permanentlyDeleteTask: Удалено строк: $rowsDeleted")
            return rowsDeleted > 0
        } catch (e: Exception) {
            println("Tasks.permanentlyDeleteTask: Общая ошибка при удалении: ${e.message}")
            e.printStackTrace()
            return false
        } finally {
            println("Tasks.permanentlyDeleteTask: Завершение операции удаления")
        }
    }
    
    private fun mapRowToTaskDTO(row: ResultRow): TaskDTO {
        return TaskDTO(
            id = row[id],
            title = row[title],
            description = row[description],
            isCompleted = row[isCompleted],
            isDeleted = row[isDeleted],
            priority = TaskPriority.valueOf(row[priority]),
            categoryId = row[categoryId] ?: "",
            userId = row[userId],
            deadline = row[deadline],
            createdAt = row[createdAt],
            updatedAt = row[updatedAt]
        )
    }
} 