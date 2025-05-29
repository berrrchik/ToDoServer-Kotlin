//package ru.todoapp.database.hidden_categories
//
//import org.jetbrains.exposed.sql.*
//import org.jetbrains.exposed.sql.javatime.datetime
//import org.jetbrains.exposed.sql.transactions.transaction
//import ru.todoapp.database.users.Users
//import ru.todoapp.database.categories.Categories
//import java.time.LocalDateTime
//import java.util.*
//import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
//
//object HiddenCategories : Table("hidden_categories") {
//    val id = varchar("id", 50)
//    val userId = varchar("user_id", 50).references(Users.id)
//    val categoryId = varchar("category_id", 50).references(Categories.id)
//    val createdAt = datetime("created_at")
//
//    override val primaryKey = PrimaryKey(id)
//
//    fun insert(userId: String, categoryId: String): HiddenCategoryDTO {
//        val id = UUID.randomUUID().toString()
//        val now = LocalDateTime.now()
//
//        val hiddenCategory = HiddenCategoryDTO(
//            id = id,
//            userId = userId,
//            categoryId = categoryId,
//            createdAt = now
//        )
//
//        transaction {
//            insert {
//                it[HiddenCategories.id] = hiddenCategory.id
//                it[HiddenCategories.userId] = hiddenCategory.userId
//                it[HiddenCategories.categoryId] = hiddenCategory.categoryId
//                it[createdAt] = hiddenCategory.createdAt
//            }
//        }
//
//        return hiddenCategory
//    }
//
//    fun isHidden(userId: String, categoryId: String): Boolean {
//        return transaction {
//            select {
//                (HiddenCategories.userId.eq(userId)) and
//                (HiddenCategories.categoryId.eq(categoryId))
//            }.count() > 0
//        }
//    }
//
//    fun delete(userId: String, categoryId: String): Boolean {
//        return try {
//            transaction {
//                deleteWhere {
//                    (HiddenCategories.userId.eq(userId)) and
//                    (HiddenCategories.categoryId.eq(categoryId))
//                }
//            }
//            true
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    fun fetchByUser(userId: String): List<HiddenCategoryDTO> {
//        return transaction {
//            select { HiddenCategories.userId.eq(userId) }
//                .map { row ->
//                    HiddenCategoryDTO(
//                        id = row[id],
//                        userId = row[HiddenCategories.userId],
//                        categoryId = row[categoryId],
//                        createdAt = row[createdAt]
//                    )
//                }
//        }
//    }
//}

package ru.todoapp.database.hidden_categories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import ru.todoapp.database.users.Users
import ru.todoapp.database.categories.Categories
import java.time.LocalDateTime
import java.util.*

object HiddenCategories : Table("hidden_categories") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(Users.id)
    val categoryId = varchar("category_id", 50).references(Categories.id)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    fun insert(userId: String, categoryId: String): HiddenCategoryDTO {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val hiddenCategory = HiddenCategoryDTO(
            id = id,
            userId = userId,
            categoryId = categoryId,
            createdAt = now
        )
        transaction {
            insert {
                it[HiddenCategories.id] = hiddenCategory.id
                it[HiddenCategories.userId] = hiddenCategory.userId
                it[HiddenCategories.categoryId] = hiddenCategory.categoryId
                it[createdAt] = hiddenCategory.createdAt
            }
        }
        return hiddenCategory
    }

    fun isHidden(userId: String, categoryId: String): Boolean {
        return transaction {
            select { (HiddenCategories.userId eq userId) and (HiddenCategories.categoryId eq categoryId) }
                .count() > 0
        }
    }

    fun delete(userId: String, categoryId: String): Boolean {
        return try {
            transaction {
                deleteWhere { (HiddenCategories.userId eq userId) and (HiddenCategories.categoryId eq categoryId) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun fetchByUser(userId: String): List<HiddenCategoryDTO> {
        return transaction {
            select { HiddenCategories.userId eq userId }
                .map { row ->
                    HiddenCategoryDTO(
                        id = row[HiddenCategories.id],
                        userId = row[HiddenCategories.userId],
                        categoryId = row[HiddenCategories.categoryId],
                        createdAt = row[HiddenCategories.createdAt]
                    )
                }
        }
    }
}