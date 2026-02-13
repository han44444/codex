package com.example.afterschoolattendance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolClassDao {
    @Query("SELECT * FROM school_class ORDER BY grade, name")
    fun observeClasses(): Flow<List<SchoolClassEntity>>

    @Insert
    suspend fun insert(item: SchoolClassEntity): Long
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM student WHERE classId = :classId ORDER BY name")
    fun observeStudents(classId: Long): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: StudentEntity): Long

    @Query("DELETE FROM student WHERE id = :studentId")
    suspend fun delete(studentId: Long)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE classId = :classId AND date = :date")
    fun observeByDate(classId: Long, date: String): Flow<List<AttendanceEntity>>

    @Upsert
    suspend fun upsert(item: AttendanceEntity)
}

@Dao
interface MessageTemplateDao {
    @Query("SELECT * FROM message_template ORDER BY name")
    fun observeTemplates(): Flow<List<MessageTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MessageTemplateEntity)
}
