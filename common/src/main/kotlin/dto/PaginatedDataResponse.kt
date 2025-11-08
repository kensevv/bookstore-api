package dto

data class PaginatedDataResponse<T>(
    val data: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)