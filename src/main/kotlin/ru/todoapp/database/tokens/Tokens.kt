//package ru.todoapp.database.tokens
//
//import org.jetbrains.exposed.sql.*
//import org.jetbrains.exposed.sql.javatime.datetime
//import org.jetbrains.exposed.sql.transactions.transaction
//import ru.todoapp.database.users.Users
//import java.time.LocalDateTime
//import java.util.*
//import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
//
//
//object Tokens : Table("tokens") {
//    val id = varchar("id", 50)
//    val login = varchar("login", 50)
//    val userId = varchar("user_id", 50).references(Users.id)
//    val token = varchar("token", 50)
//    val createdAt = datetime("created_at")
//
//    override val primaryKey = PrimaryKey(id)
//
//    fun insert(tokenDTO: TokenDTO) {
//        val now = LocalDateTime.now()
//
//        transaction {
//            insert {
//                it[id] = tokenDTO.rowId
//                it[login] = tokenDTO.login
//                it[userId] = tokenDTO.userId
//                it[token] = tokenDTO.token
//                it[createdAt] = now
//            }
//        }
//    }
//
//    fun findByToken(token: String): TokenDTO? {
//        return try {
//            transaction {
//                select { Tokens.token.eq(token) }
//                    .singleOrNull()
//                    ?.let {
//                        TokenDTO(
//                            rowId = it[id],
//                            login = it[login],
//                            userId = it[userId],
//                            token = it[token],
//                            createdAt = it[createdAt]
//                        )
//                    }
//            }
//        } catch (e: Exception) {
//            null
//        }
//    }
//}

package ru.todoapp.database.tokens

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import ru.todoapp.database.users.Users
import java.time.LocalDateTime
import java.util.*

object Tokens : Table("tokens") {
    val id = varchar("id", 50)
    val login = varchar("login", 50)
    val userId = varchar("user_id", 50).references(Users.id)
    val token = varchar("token", 50)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    fun insert(tokenDTO: TokenDTO) {
        val now = LocalDateTime.now()
        transaction {
            insert {
                it[id] = tokenDTO.rowId
                it[login] = tokenDTO.login
                it[userId] = tokenDTO.userId
                it[token] = tokenDTO.token
                it[createdAt] = now
            }
        }
    }

    fun findByToken(token: String): TokenDTO? {
        return try {
            transaction {
                select { Tokens.token eq token } // Исправлено на инфиксную нотацию
                    .singleOrNull()
                    ?.let {
                        TokenDTO(
                            rowId = it[Tokens.id],
                            login = it[login],
                            userId = it[userId],
                            token = it[Tokens.token],
                            createdAt = it[createdAt]
                        )
                    }
            }
        } catch (e: Exception) {
            null
        }
    }
}