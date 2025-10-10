package com.lvlup.backend.repository

import com.lvlup.backend.model.User
import com.lvlup.backend.model.UserRole
import com.lvlup.bookstore.jooq.tables.Users.Companion.USERS
import com.lvlup.bookstore.jooq.tables.records.UsersRecord
import org.jooq.DSLContext
import org.jooq.InsertValuesStepN
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import kotlin.String

@Repository
class UserRepository(private var db: DSLContext) {

    fun findByUsername(username: String): User? = fetchOneUserRecordByUsername(username)?.mapToUser()

    fun existsByUsername(username: String): Boolean = fetchOneUserRecordByUsername(username) != null

    fun createUser(user: User): Int {
        val record = db.newRecord(USERS).apply {
            username = user.username
            passwordHash = user.passwordHash
            createdAt = user.createdAt
            updatedAt = user.updatedAt
            role = user.role.name
        }
        return record.store()
    }

    private fun fetchOneUserRecordByUsername(username: String) =
        db.fetchOne(USERS, USERS.USERNAME.eq(username))

    private fun UsersRecord.mapToUser(): User {
        val currentTime = LocalDateTime.now()
        return User(
            username = this.username!!,
            passwordHash = this.passwordHash!!,
            role = UserRole.valueOf(this.role!!),
            createdAt = currentTime,
            updatedAt = currentTime,
            deleted = this.deleted == "Y"
        )
    }
}
