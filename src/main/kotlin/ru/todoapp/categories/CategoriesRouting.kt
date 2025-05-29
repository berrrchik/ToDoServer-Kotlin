package ru.todoapp.categories

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureCategoriesRouting() {
    routing {
        route("/categories") {
            // Получение всех категорий пользователя
            get {
                val categoriesController = CategoriesController(call)
                categoriesController.getCategories()
            }
            
            // Создание новой категории
            post {
                val categoriesController = CategoriesController(call)
                categoriesController.createCategory()
            }
            
            // Получение категории по ID
            get("/{id}") {
                val categoriesController = CategoriesController(call)
                categoriesController.getCategoryById()
            }
            
            // Обновление категории
            put {
                val categoriesController = CategoriesController(call)
                categoriesController.updateCategory()
            }
            
            // Удаление категории (через URL-параметр)
            delete("/{id}") {
                val categoriesController = CategoriesController(call)
                categoriesController.deleteCategory()
            }
            
            // Альтернативный способ удаления категории (через POST-запрос)
            post("/delete/{id}") {
                val categoriesController = CategoriesController(call)
                categoriesController.deleteCategory()
            }
            
            // Скрыть категорию
            post("/hide") {
                val categoriesController = CategoriesController(call)
                categoriesController.hideCategory()
            }
            
            // Показать категорию
            post("/unhide/{id}") {
                val categoriesController = CategoriesController(call)
                categoriesController.unhideCategory()
            }
            
            // Получить скрытые категории
            get("/hidden") {
                val categoriesController = CategoriesController(call)
                categoriesController.getHiddenCategories()
            }
            
            // Ручное исправление категорий (удаление дубликатов и т.д.)
            post("/fix") {
                val categoriesController = CategoriesController(call)
                categoriesController.fixCategories()
            }
            
            // Синхронизация флагов скрытых категорий
            post("/sync-hidden") {
                val categoriesController = CategoriesController(call)
                categoriesController.syncHiddenCategories()
            }
        }
    }
} 