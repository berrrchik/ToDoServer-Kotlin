package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import ru.todoapp.module
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        environment {
            // Указываем путь к тестовой конфигурации
            config = "application.conf"
        }
        
        application {
            module()
        }
        
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("TodoApp API работает", response.bodyAsText())
    }

}
