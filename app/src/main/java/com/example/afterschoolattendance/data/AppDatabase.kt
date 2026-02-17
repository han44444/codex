package com.example.afterschoolattendance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class AttendanceStatusConverter {
    @TypeConverter
    fun fromStatus(value: AttendanceStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)
}

@Database(
    entities = [
        SchoolClassEntity::class,
        StudentEntity::class,
        AttendanceEntity::class,
        MessageTemplateEntity::class,
    ],
    version = 1,
)
@TypeConverters(AttendanceStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classDao(): SchoolClassDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun messageTemplateDao(): MessageTemplateDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "attendance.db",
            ).build()
        }
    }
}
