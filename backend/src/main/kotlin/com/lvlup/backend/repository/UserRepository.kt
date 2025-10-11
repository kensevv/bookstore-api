package com.lvlup.backend.repository

import com.lvlup.backend.model.User
import com.lvlup.backend.model.UserRole
import com.lvlup.bookstore.jooq.tables.Users.Companion.USERS
import com.lvlup.bookstore.jooq.tables.records.UsersRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.String

@Repository
class UserRepository(private var db: DSLContext) {

    fun findByEmail(email: String): User? = fetchOneUserRecordByEmail(email)?.mapToUser()

    fun existsByEmail(email: String): Boolean = fetchOneUserRecordByEmail(email) != null

    @Transactional
    fun createUser(user: User): Int {
        val record = db.newRecord(USERS).apply {
            email = user.email
            firstName = user.firstName
            lastName = user.lastName
            passwordHash = user.passwordHash
            createdAt = user.createdAt
            updatedAt = user.updatedAt
            role = user.role.name
        }
        return record.store()
    }

    @Transactional
    fun updatePassword(userEmail: String, newPasswordHash: String): Int? {
        return fetchOneUserRecordByEmail(userEmail)?.apply {
            passwordHash = newPasswordHash
            updatedAt = LocalDateTime.now()
        }?.update()
    }

    @Transactional
    fun updateUserProfile(userEmail: String, newFirstName: String, newLastName: String): User? {
        val updatedUserResponse = fetchOneUserRecordByEmail(userEmail)?.apply {
            firstName = newFirstName
            lastName = newLastName
            updatedAt = LocalDateTime.now()
        }.let { updatedRecord ->
            updatedRecord?.update()
            updatedRecord?.mapToUser()
        }
        return updatedUserResponse
    }

    private fun fetchOneUserRecordByEmail(email: String) =
        db.fetchOne(USERS, USERS.EMAIL.eq(email))

    private fun UsersRecord.mapToUser(): User = User(
        email = this.email!!,
        firstName = this.firstName!!,
        lastName = this.lastName!!,
        passwordHash = this.passwordHash!!,
        role = UserRole.valueOf(this.role!!),
        createdAt = this.createdAt!!,
        updatedAt = this.updatedAt!!,
        deleted = this.deleted!!
    )

}
