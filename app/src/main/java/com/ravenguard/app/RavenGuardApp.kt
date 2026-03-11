package com.ravenguard.app

import android.app.Application
import com.ravenguard.app.data.database.ContactDatabase
import com.ravenguard.app.data.repository.ContactRepository

class RavenGuardApp : Application() {
    lateinit var contactRepository: ContactRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = ContactDatabase.getDatabase(this)
        contactRepository = ContactRepository(db.contactDao())
    }
}
