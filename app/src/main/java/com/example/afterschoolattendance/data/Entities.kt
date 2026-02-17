package com.example.afterschoolattendance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "school_class")
data class SchoolClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val grade: String,
    val description: String = ""
)

@Entity(
    tableName = "student",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"]), Index(value = ["classId", "name", "parentPhone"], unique = true)]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val name: String,
    val parentPhone: String,
    val sourceContactId: String? = null
)

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
}

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index(value = ["classId"]), Index(value = ["studentId"]), Index(value = ["date", "studentId"], unique = true)]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val studentId: Long,
    val date: String,
    val status: AttendanceStatus,
    val memo: String = ""
)

@Entity(tableName = "message_template")
data class MessageTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val body: String,
)
