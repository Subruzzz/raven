package com.ravenguard.app.data.repository

import android.telephony.SmsManager
import com.ravenguard.app.data.database.ContactEntity

class SmsRepository {
    fun sendEmergencySms(contacts: List<ContactEntity>, message: String): Result<Unit> {
        return runCatching {
            val smsManager = SmsManager.getDefault()
            contacts.forEach { contact ->
                val content = buildString {
                    append(message)
                    if (contact.message.isNotBlank()) {
                        append("\n")
                        append(contact.message)
                    }
                }
                smsManager.sendTextMessage(contact.phoneNumber, null, content, null, null)
            }
        }
    }
}
