package com.lvlup.inventoryservice.service

import com.lvlup.inventoryservice.dto.OrderResponse
import com.lvlup.inventoryservice.model.Order
import dto.PaginatedDataResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import kotlin.reflect.KProperty1

fun <T> sortByKProperty(
    property: KProperty1<T, *>,
    direction: Sort.Direction = Sort.Direction.ASC
): Sort = Sort.by(direction, property.name)

fun <T, R> Page<T>.toPaginatedDataResponse(
    data: List<R>? = null,
): PaginatedDataResponse<R> {
    return PaginatedDataResponse(
        data = data ?: this.content as List<R>,
        totalElements = this.totalElements,
        totalPages = this.totalPages,
        currentPage = this.number,
        pageSize = this.size
    )
}