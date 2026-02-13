package com.example.afterschoolattendance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.afterschoolattendance.data.AttendanceStatus
import com.example.afterschoolattendance.data.StudentEntity
import com.example.afterschoolattendance.domain.AttendanceRepository
import com.example.afterschoolattendance.sms.SmsSender
import com.example.afterschoolattendance.sms.buildMessage
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val selectedClassId: Long? = null,
    val selectedDate: String = LocalDate.now().toString(),
    val messageBody: String = "학생이 출결하지 않았습니다.",
    val markedAttendance: Map<Long, AttendanceStatus> = emptyMap(),
)

class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val smsSender: SmsSender,
) : ViewModel() {
    private val mutableState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableState

    val classes = repository.observeClasses().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun observeStudents(classId: Long) = repository.observeStudents(classId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun selectClass(classId: Long) {
        mutableState.value = mutableState.value.copy(
            selectedClassId = classId,
            markedAttendance = emptyMap(),
        )
    }

    fun setMessageBody(body: String) {
        mutableState.value = mutableState.value.copy(messageBody = body)
    }

    fun addSampleClass() {
        viewModelScope.launch {
            if (classes.value.isNotEmpty()) return@launch
            val classId = repository.addClass(name = "A반", grade = "3학년")
            repository.addStudent(classId, "길길", "01012345678")
            repository.addStudent(classId, "민민", "01099998888")
            selectClass(classId)
        }
    }

    fun addClass(grade: String, name: String) {
        viewModelScope.launch {
            val classId = repository.addClass(name = name, grade = grade)
            selectClass(classId)
        }
    }

    fun addStudent(name: String, phone: String) {
        val classId = uiState.value.selectedClassId ?: return
        viewModelScope.launch {
            repository.addStudent(classId, name, phone)
        }
    }

    fun deleteStudent(studentId: Long) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            mutableState.value = mutableState.value.copy(
                markedAttendance = mutableState.value.markedAttendance - studentId,
            )
        }
    }

    fun mark(studentId: Long, status: AttendanceStatus) {
        val state = uiState.value
        val classId = state.selectedClassId ?: return

        mutableState.value = state.copy(markedAttendance = state.markedAttendance + (studentId to status))

        viewModelScope.launch {
            repository.markAttendance(
                classId = classId,
                studentId = studentId,
                date = state.selectedDate,
                status = status,
            )
        }
    }

    fun sendToStudent(student: StudentEntity) {
        val message = buildMessage(student.name, uiState.value.messageBody)
        smsSender.send(student.parentPhone, message)
    }

    fun sendBulk(students: List<StudentEntity>) {
        val body = uiState.value.messageBody
        students.forEach { student ->
            smsSender.send(student.parentPhone, buildMessage(student.name, body))
        }
    }

    fun sendBulkAbsent(students: List<StudentEntity>) {
        val marked = uiState.value.markedAttendance
        val absentStudents = students.filter { marked[it.id] == AttendanceStatus.ABSENT }
        sendBulk(absentStudents)
    }
}

class AttendanceViewModelFactory(
    private val repository: AttendanceRepository,
    private val smsSender: SmsSender,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository, smsSender) as T
        }
        error("Unknown ViewModel class: $modelClass")
    }
}
