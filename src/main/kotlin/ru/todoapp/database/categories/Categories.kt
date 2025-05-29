package ru.todoapp.database.categories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import ru.todoapp.database.users.Users
import java.time.LocalDateTime
import java.util.*

object Categories : Table("categories") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val isDefault = bool("is_default").default(false)
    val isHidden = bool("is_hidden").default(false)
    val userId = varchar("user_id", 50).references(Users.id)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    fun insert(categoryCreateDTO: CategoryCreateDTO): CategoryDTO {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        
        val category = CategoryDTO(
            id = id,
            name = categoryCreateDTO.name,
            isDefault = categoryCreateDTO.isDefault,
            isHidden = false,
            userId = categoryCreateDTO.userId,
            createdAt = now
        )
        
        transaction {
            insert {
                it[Categories.id] = category.id
                it[name] = category.name
                it[isDefault] = category.isDefault
                it[isHidden] = category.isHidden
                it[userId] = category.userId
                it[createdAt] = category.createdAt
            }
        }
        
        return category
    }
    
    fun fetchCategoriesByUser(userId: String): List<CategoryDTO> {
        return transaction {
            select { Categories.userId eq userId }
                .map { row ->
                    mapRowToCategoryDTO(row)
                }
        }
    }
    
    fun fetchCategoryById(id: String): CategoryDTO? {
        return try {
            transaction {
                select { Categories.id.eq(id) }
                    .singleOrNull()
                    ?.let { mapRowToCategoryDTO(it) }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun updateCategory(category: CategoryDTO): Boolean {
        return try {
            transaction {
                update({ Categories.id.eq(category.id) }) {
                    it[name] = category.name
                    it[isHidden] = category.isHidden
                    it[isDefault] = category.isDefault
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteCategory(id: String): Boolean {
        println("Начало процесса физического удаления категории с ID: $id")
        
        try {
            transaction {
                // Сначала обновляем задачи, которые ссылаются на эту категорию
                ru.todoapp.database.tasks.Tasks.update({ ru.todoapp.database.tasks.Tasks.categoryId eq id }) {
                    it[ru.todoapp.database.tasks.Tasks.categoryId] = null
                }
                
                // Удаляем записи из HiddenCategories, если они есть
                ru.todoapp.database.hidden_categories.HiddenCategories.deleteWhere {
                    ru.todoapp.database.hidden_categories.HiddenCategories.categoryId eq id
                }
                
                // Физическое удаление категории
                deleteWhere { Categories.id eq id }
            }
            
            println("Категория физически удалена из базы данных")
            return true
        } catch (e: Exception) {
            println("Ошибка при удалении категории: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    fun createDefaultCategories(userId: String) {
        val defaultCategories = listOf("ДОМ", "РАБОТА", "УЧЁБА", "ДРУГОЕ")
        
        transaction {
            defaultCategories.forEach { categoryName ->
                // Проверяем, существует ли уже такая категория у пользователя
                val existingCategory = select { 
                    (Categories.name eq categoryName) and (Categories.userId eq userId) 
                }.singleOrNull()
                
                if (existingCategory == null) {
                    // Создаем категорию только если она не существует
                    insert(
                        CategoryCreateDTO(
                            name = categoryName,
                            isDefault = true,
                            userId = userId
                        )
                    )
                }
            }
        }
    }
    
    fun fixDefaultCategories() {
        val defaultCategoryNames = listOf("ДОМ", "РАБОТА", "УЧЁБА", "ДРУГОЕ")
        
        transaction {
            // Получаем всех пользователей
            val users = ru.todoapp.database.users.Users.selectAll().map { it[ru.todoapp.database.users.Users.id] }
            
            // Для каждого пользователя исправляем категории
            users.forEach { userId ->
                defaultCategoryNames.forEach { categoryName ->
                    // Находим все категории с таким именем для конкретного пользователя
                    val userCategories = select { 
                        (Categories.name eq categoryName) and (Categories.userId eq userId) 
                    }.map { it[Categories.id] }
                    
                    if (userCategories.isEmpty()) {
                        // Если у пользователя нет такой категории, создаем её
                        insert(
                            CategoryCreateDTO(
                                name = categoryName,
                                isDefault = true,
                                userId = userId
                            )
                        )
                    } else if (userCategories.size > 1) {
                        // Если есть дубликаты, оставляем только первую
                        for (i in 1 until userCategories.size) {
                            deleteWhere { Categories.id eq userCategories[i] }
                        }
                        
                        // Убеждаемся, что оставшаяся категория помечена как стандартная
                        update({ Categories.id eq userCategories[0] }) {
                            it[isDefault] = true
                        }
                    } else {
                        // Если есть только одна категория, убеждаемся что она стандартная
                        update({ Categories.id eq userCategories[0] }) {
                            it[isDefault] = true
                        }
                    }
                }
            }
            
            // Синхронизируем флаг isHidden со скрытыми категориями
            syncHiddenFlag()
        }
    }
    
    fun syncHiddenFlag() {
        transaction {
            // Сначала сбрасываем все флаги isHidden
            update({ Categories.id.isNotNull() }) {
                it[isHidden] = false
            }
            
            // Получаем все скрытые категории из таблицы HiddenCategories
            val hiddenCategories = ru.todoapp.database.hidden_categories.HiddenCategories
                .selectAll()
                .map { row -> 
                    Pair(
                        row[ru.todoapp.database.hidden_categories.HiddenCategories.userId], 
                        row[ru.todoapp.database.hidden_categories.HiddenCategories.categoryId]
                    ) 
                }
            
            // Устанавливаем флаг isHidden для каждой скрытой категории
            hiddenCategories.forEach { (userId, categoryId) ->
                update({ (Categories.id eq categoryId) and (Categories.userId eq userId) }) {
                    it[isHidden] = true
                }
            }
        }
    }
    
    private fun mapRowToCategoryDTO(row: ResultRow): CategoryDTO {
        return CategoryDTO(
            id = row[id],
            name = row[name],
            isDefault = row[isDefault],
            isHidden = row[isHidden],
            userId = row[userId],
            createdAt = row[createdAt]
        )
    }
} 