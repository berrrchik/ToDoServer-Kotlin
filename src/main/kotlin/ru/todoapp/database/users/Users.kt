package ru.todoapp.database.users

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

object Users : Table("users") {
    val id = varchar("id", 50)
    val login = varchar("login", 25)
    val password = varchar("password", 25)
    val username = varchar("username", 25)
    val email = varchar("email", 25)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    fun insert(userDTO: UserDTO): String {
        transaction {
            insert {
                it[id] = userDTO.id
                it[login] = userDTO.login
                it[password] = userDTO.password
                it[username] = userDTO.username
                it[email] = userDTO.email
                it[createdAt] = userDTO.createdAt
            }
        }
        return userDTO.id
    }

    fun fetchUser(userLogin: String): UserDTO? {
        return try {
            transaction {
                val query = Users.select { Users.login eq userLogin }
                query.singleOrNull()?.let {
                    UserDTO(
                        id = it[Users.id],        // Явная ссылка на колонку Users.id
                        login = it[Users.login],  // Явная ссылка на колонку Users.login
                        password = it[Users.password],  // Явная ссылка на колонку Users.password
                        username = it[Users.username],  // Явная ссылка на колонку Users.username
                        email = it[Users.email],  // Явная ссылка на колонку Users.email
                        createdAt = it[Users.createdAt]  // Явная ссылка на колонку Users.createdAt
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun fetchUserById(userId: String): UserDTO? {
        return try {
            transaction {
                val query = Users.select { Users.id eq userId }
                query.singleOrNull()?.let {
                    UserDTO(
                        id = it[Users.id],        // Явная ссылка на колонку Users.id
                        login = it[Users.login],  // Явная ссылка на колонку Users.login
                        password = it[Users.password],  // Явная ссылка на колонку Users.password
                        username = it[Users.username],  // Явная ссылка на колонку Users.username
                        email = it[Users.email],  // Явная ссылка на колонку Users.email
                        createdAt = it[Users.createdAt]  // Явная ссылка на колонку Users.createdAt
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}