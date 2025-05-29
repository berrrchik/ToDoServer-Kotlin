package ru.todoapp.categories

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import ru.todoapp.database.categories.Categories
import ru.todoapp.database.categories.CategoryCreateDTO
import ru.todoapp.database.categories.CategoryDTO
import ru.todoapp.database.hidden_categories.HiddenCategories
import ru.todoapp.database.tokens.Tokens
import java.time.format.DateTimeFormatter

class CategoriesController(private val call: ApplicationCall) {
    
    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    suspend fun createCategory() {
        // Отладочная информация о запросе
        println("Получен запрос на создание категории")
        println("Метод: ${call.request.httpMethod}")
        println("URI: ${call.request.uri}")
        println("Заголовки: ${call.request.headers.entries().map { "${it.key}=${it.value}" }}")
        
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        println("Получен токен: $token")
        
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        println("Токен проверен, пользователь: ${tokenDTO.userId}")
        
        try {
            val receiveCategory = call.receive<CategoryReceiveRemote>()
            println("Получены данные категории: ${receiveCategory.name}, isDefault=${receiveCategory.isDefault}")
            
            val categoryCreateDTO = CategoryCreateDTO(
                name = receiveCategory.name,
                isDefault = receiveCategory.isDefault,
                userId = tokenDTO.userId
            )
            
            println("Создание категории...")
            val createdCategory = Categories.insert(categoryCreateDTO)
            
            println("Категория успешно создана с ID: ${createdCategory.id}")
            call.respond(HttpStatusCode.Created, mapCategoryToResponse(createdCategory))
        } catch (e: Exception) {
            println("Ошибка при создании категории: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Не удалось создать категорию: ${e.message}"))
        }
    }
    
    suspend fun getCategories() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val categories = Categories.fetchCategoriesByUser(tokenDTO.userId)
        
        val hiddenCategoriesIds = HiddenCategories.fetchByUser(tokenDTO.userId).map { it.categoryId }
        
        val visibleCategories = categories.filter { it.id !in hiddenCategoriesIds }
        
        call.respond(CategoryListResponseRemote(
            categories = visibleCategories.map { mapCategoryToResponse(it) }
        ))
    }
    
    suspend fun getCategoryById() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val categoryId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID категории не указан")
        
        val category = Categories.fetchCategoryById(categoryId) ?: return call.respond(HttpStatusCode.NotFound, "Категория не найдена")
        
        if (category.userId != tokenDTO.userId) {
            return call.respond(HttpStatusCode.Forbidden, "Доступ запрещен")
        }
        
        call.respond(mapCategoryToResponse(category))
    }
    
    suspend fun updateCategory() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val updateCategory = call.receive<CategoryUpdateRemote>()
        val categoryId = updateCategory.id
        
        val category = Categories.fetchCategoryById(categoryId) ?: return call.respond(HttpStatusCode.NotFound, "Категория не найдена")
        
        if (category.userId != tokenDTO.userId) {
            return call.respond(HttpStatusCode.Forbidden, "Доступ запрещен")
        }
        
        if (category.isDefault && updateCategory.name != null) {
            return call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Нельзя изменить имя стандартной категории"))
        }
        
        if (category.isDefault && updateCategory.isDefault != null && !updateCategory.isDefault) {
            return call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Нельзя изменить флаг isDefault для стандартной категории"))
        }
        
        try {
            val updatedCategory = CategoryDTO(
                id = category.id,
                name = updateCategory.name ?: category.name,
                isDefault = updateCategory.isDefault ?: category.isDefault,
                isHidden = updateCategory.isHidden ?: category.isHidden,
                userId = category.userId,
                createdAt = category.createdAt
            )
            
            val success = Categories.updateCategory(updatedCategory)
            
            if (success) {
                call.respond(mapCategoryToResponse(updatedCategory))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Не удалось обновить категорию"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Не удалось обновить категорию: ${e.message}"))
        }
    }
    
    suspend fun deleteCategory() {
        // Отладочная информация о запросе
        println("Получен запрос на удаление категории")
        println("Метод: ${call.request.httpMethod}")
        println("URI: ${call.request.uri}")
        println("Параметры: ${call.parameters.entries().map { "${it.key}=${it.value}" }}")
        println("Заголовки: ${call.request.headers.entries().map { "${it.key}=${it.value}" }}")
        
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Токен не предоставлен"))
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Недействительный токен"))
        
        val categoryId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "ID категории не указан"))
        println("Попытка удаления категории с ID: $categoryId")
        
        // Проверим наличие категории перед удалением
        val category = Categories.fetchCategoryById(categoryId)
        
        if (category == null) {
            println("Категория с ID $categoryId не найдена")
            return call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Категория не найдена"))
        }
        
        println("Категория найдена: ${category.name}")
        
        if (category.userId != tokenDTO.userId) {
            println("Отказано в доступе: ID пользователя категории (${category.userId}) не совпадает с ID текущего пользователя (${tokenDTO.userId})")
            return call.respond(HttpStatusCode.Forbidden, mapOf("success" to false, "message" to "Доступ запрещен"))
        }
        
        if (category.isDefault) {
            println("Попытка удалить стандартную категорию")
            return call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Нельзя удалить стандартную категорию"))
        }
        
        println("Вызов метода Categories.deleteCategory")
        val success = Categories.deleteCategory(categoryId)
        
        if (success) {
            println("Категория успешно удалена")
            call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Категория успешно удалена"))
        } else {
            println("Не удалось удалить категорию")
            call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Не удалось удалить категорию"))
        }
    }
    
    suspend fun hideCategory() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val request = call.receive<HideCategoryRequest>()
        val categoryId = request.categoryId
        
        val category = Categories.fetchCategoryById(categoryId) ?: return call.respond(HttpStatusCode.NotFound, "Категория не найдена")
        
        if (category.userId != tokenDTO.userId) {
            return call.respond(HttpStatusCode.Forbidden, "Доступ запрещен")
        }
        
        val isAlreadyHidden = HiddenCategories.isHidden(tokenDTO.userId, categoryId)
        
        if (isAlreadyHidden) {
            call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Категория уже скрыта"))
            return
        }
        
        try {
            val hiddenCategory = HiddenCategories.insert(tokenDTO.userId, categoryId)
            
            val updatedCategory = CategoryDTO(
                id = category.id,
                name = category.name,
                isDefault = category.isDefault,
                isHidden = true,
                userId = category.userId,
                createdAt = category.createdAt
            )
            Categories.updateCategory(updatedCategory)
            
            call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Категория успешно скрыта"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Не удалось скрыть категорию: ${e.message}"))
        }
    }
    
    suspend fun unhideCategory() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val categoryId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID категории не указан")
        
        val category = Categories.fetchCategoryById(categoryId) ?: return call.respond(HttpStatusCode.NotFound, "Категория не найдена")
        
        if (category.userId != tokenDTO.userId) {
            return call.respond(HttpStatusCode.Forbidden, "Доступ запрещен")
        }
        
        if (!HiddenCategories.isHidden(tokenDTO.userId, categoryId)) {
            call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Категория уже видима"))
            return
        }
        
        try {
            val success = HiddenCategories.delete(tokenDTO.userId, categoryId)
            
            if (success) {
                val updatedCategory = CategoryDTO(
                    id = category.id,
                    name = category.name,
                    isDefault = category.isDefault,
                    isHidden = false,
                    userId = category.userId,
                    createdAt = category.createdAt
                )
                Categories.updateCategory(updatedCategory)
                
                call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Категория успешно отображена"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Не удалось отобразить категорию"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "message" to "Не удалось отобразить категорию: ${e.message}"))
        }
    }
    
    suspend fun getHiddenCategories() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        val hiddenCategoriesIds = HiddenCategories.fetchByUser(tokenDTO.userId).map { it.categoryId }
        
        val hiddenCategories = Categories.fetchCategoriesByUser(tokenDTO.userId)
            .filter { it.id in hiddenCategoriesIds }
        
        call.respond(CategoryListResponseRemote(
            categories = hiddenCategories.map { mapCategoryToResponse(it) }
        ))
    }
    
    suspend fun fixCategories() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        Categories.fixDefaultCategories()
        
        call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Категории успешно исправлены"))
    }
    
    suspend fun syncHiddenCategories() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: return call.respond(HttpStatusCode.Unauthorized, "Токен не предоставлен")
        val tokenDTO = Tokens.findByToken(token) ?: return call.respond(HttpStatusCode.Unauthorized, "Недействительный токен")
        
        Categories.syncHiddenFlag()
        
        call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Скрытые категории синхронизированы"))
    }
    
    private fun mapCategoryToResponse(category: CategoryDTO): CategoryResponseRemote {
        return CategoryResponseRemote(
            id = category.id,
            name = category.name,
            isDefault = category.isDefault,
            isHidden = category.isHidden,
            createdAt = category.createdAt.format(dateFormatter)
        )
    }
} 