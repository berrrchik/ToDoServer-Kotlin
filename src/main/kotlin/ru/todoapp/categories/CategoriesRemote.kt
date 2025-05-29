package ru.todoapp.categories

import kotlinx.serialization.Serializable

@Serializable
data class CategoryReceiveRemote(
    val name: String,
    val isDefault: Boolean = false
)

@Serializable
data class CategoryResponseRemote(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val isHidden: Boolean,
    val createdAt: String
)

@Serializable
data class CategoryUpdateRemote(
    val id: String,
    val name: String? = null,
    val isHidden: Boolean? = null,
    val isDefault: Boolean? = null
)

@Serializable
data class CategoryListResponseRemote(
    val categories: List<CategoryResponseRemote>
)

@Serializable
data class HideCategoryRequest(
    val categoryId: String
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String
) 