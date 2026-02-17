package com.example.afterschoolattendance.sms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager

interface SmsSender {
    fun send(phone: String, message: String)
}

class SmsManagerSender : SmsSender {
    override fun send(phone: String, message: String) {
        SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
    }
}

class SmsIntentSender(
    private val context: Context,
) : SmsSender {
    override fun send(phone: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

fun buildMessage(studentName: String, body: String): String {
    return "${studentName}학생 $body".trim()
}
