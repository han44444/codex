package com.example.afterschoolattendance.contacts

import android.content.ContentResolver
import android.provider.ContactsContract

data class ContactItem(
    val contactId: String,
    val name: String,
    val phone: String,
)

class ContactReader(
    private val contentResolver: ContentResolver,
) {
    fun loadContacts(): List<ContactItem> {
        val items = mutableListOf<ContactItem>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )

        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val normalized = normalizePhone(cursor.getString(numberColumn))
                if (normalized.length < 9) continue

                items.add(
                    ContactItem(
                        contactId = cursor.getString(idColumn),
                        name = cursor.getString(nameColumn) ?: "",
                        phone = normalized,
                    )
                )
            }
        }

        return items
    }

    private fun normalizePhone(phone: String): String {
        return phone.filter { it.isDigit() || it == '+' }
    }
}
