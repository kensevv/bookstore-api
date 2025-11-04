package com.lvlup.inventoryservice.repository

import org.jooq.UpdatableRecord

fun <T : UpdatableRecord<T>?> UpdatableRecord<T>.nullIfPrimaryKeyIsNull() =
    this.takeIf { it.key().fields().none { field -> field.getValue(this) == null } }
