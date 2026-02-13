package com.example.afterschoolattendance.domain

import com.example.afterschoolattendance.contacts.ContactItem
import com.example.afterschoolattendance.data.AppDatabase
import com.example.afterschoolattendance.data.AttendanceEntity
import com.example.afterschoolattendance.data.AttendanceStatus
import com.example.afterschoolattendance.data.SchoolClassEntity
import com.example.afterschoolattendance.data.StudentEntity
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val db: AppDatabase,
) {
    fun observeClasses() = db.classDao().observeClasses()

    fun observeStudents(classId: Long) = db.studentDao().observeStudents(classId)

    fun observeAttendanceByDate(classId: Long, date: String) =
        db.attendanceDao().observeByDate(classId, date)

    suspend fun addClass(name: String, grade: String, description: String = ""): Long {
        return db.classDao().insert(SchoolClassEntity(name = name, grade = grade, description = description))
    }

    suspend fun addStudent(classId: Long, name: String, parentPhone: String, sourceContactId: String? = null) {
        db.studentDao().insert(
            StudentEntity(
                classId = classId,
                name = name,
                parentPhone = normalizePhone(parentPhone),
                sourceContactId = sourceContactId,
            )
        )
    }

    suspend fun importContact(classId: Long, contact: ContactItem) {
        addStudent(
            classId = classId,
            name = contact.name,
            parentPhone = contact.phone,
            sourceContactId = contact.contactId,
        )
    }

    suspend fun deleteStudent(studentId: Long) {
        db.studentDao().delete(studentId)
    }

    suspend fun markAttendance(
        classId: Long,
        studentId: Long,
        date: String,
        status: AttendanceStatus,
        memo: String = "",
    ) {
        db.attendanceDao().upsert(
            AttendanceEntity(
                classId = classId,
                studentId = studentId,
                date = date,
                status = status,
                memo = memo,
            )
        )
    }

    private fun normalizePhone(phone: String): String {
        return phone.filter { it.isDigit() || it == '+' }
    }
}
