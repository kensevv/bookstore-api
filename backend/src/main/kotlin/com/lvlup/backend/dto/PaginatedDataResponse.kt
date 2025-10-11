package com.lvlup.backend.dto

data class PaginatedDataResponse<T>(
    val data: List<T>,
    val totalElements: Int,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)