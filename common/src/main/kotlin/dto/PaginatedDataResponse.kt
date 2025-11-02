package dto


data class PaginatedDataResponse<T>(
    val data: List<T>,
    val totalElements: Long,
    val totalPages: Long,
    val currentPage: Int,
    val pageSize: Int
)