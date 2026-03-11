package com.ravenguard.app.data.repository

import com.ravenguard.app.data.database.ContactDao
import com.ravenguard.app.data.database.ContactEntity
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {
    fun observeContacts(): Flow<List<ContactEntity>> = contactDao.observeContacts()

    suspend fun addContact(contact: ContactEntity): Result<Unit> {
        return runCatching {
            if (contactDao.countContacts() >= MAX_CONTACTS) {
                error("Maximum of $MAX_CONTACTS contacts allowed")
            }
            contactDao.insert(contact)
        }
    }

    suspend fun removeContact(contact: ContactEntity) {
        contactDao.delete(contact)
    }

    companion object {
        const val MAX_CONTACTS = 5
    }
}
