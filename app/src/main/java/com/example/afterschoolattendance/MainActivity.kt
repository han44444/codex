package com.example.afterschoolattendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.afterschoolattendance.data.AppDatabase
import com.example.afterschoolattendance.data.AttendanceStatus
import com.example.afterschoolattendance.data.SchoolClassEntity
import com.example.afterschoolattendance.data.StudentEntity
import com.example.afterschoolattendance.domain.AttendanceRepository
import com.example.afterschoolattendance.sms.SmsIntentSender
import com.example.afterschoolattendance.ui.AttendanceViewModel
import com.example.afterschoolattendance.ui.AttendanceViewModelFactory

class MainActivity : ComponentActivity() {
    private val db by lazy { AppDatabase.create(this) }
    private val repository by lazy { AttendanceRepository(db) }
    private val viewModel by viewModels<AttendanceViewModel> {
        AttendanceViewModelFactory(repository, SmsIntentSender(this))
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()

        setContent {
            MaterialTheme {
                AttendanceApp(viewModel)
            }
        }
    }

    private fun requestRuntimePermissions() {
        val required = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}

@Composable
private fun AttendanceApp(viewModel: AttendanceViewModel) {
    val classes by viewModel.classes.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val selectedClass = classes.firstOrNull { it.id == state.selectedClassId }
    val studentsFlowClassId = selectedClass?.id

    var classGrade by remember { mutableStateOf("3학년") }
    var className by remember { mutableStateOf("A반") }
    var studentName by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf("") }

    LaunchedEffect(classes) {
        if (classes.isEmpty()) viewModel.addSampleClass()
        else if (state.selectedClassId == null) viewModel.selectClass(classes.first().id)
    }

    val students = if (studentsFlowClassId != null) {
        viewModel.observeStudents(studentsFlowClassId).collectAsState().value
    } else {
        emptyList()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("학급 출결 관리", style = MaterialTheme.typography.headlineSmall)

            ClassPicker(
                classes = classes,
                selected = selectedClass,
                onSelected = viewModel::selectClass,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = classGrade,
                    onValueChange = { classGrade = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("학년") },
                )
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("반") },
                )
                Button(onClick = { viewModel.addClass(classGrade, className) }) { Text("학급 추가") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("학생 이름") },
                )
                OutlinedTextField(
                    value = studentPhone,
                    onValueChange = { studentPhone = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("보호자 번호") },
                )
                Button(
                    onClick = {
                        viewModel.addStudent(studentName, studentPhone)
                        studentName = ""
                        studentPhone = ""
                    }
                ) { Text("학생 추가") }
            }

            OutlinedTextField(
                value = state.messageBody,
                onValueChange = viewModel::setMessageBody,
                label = { Text("문자 내용(이름은 자동으로 앞에 붙음)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.sendBulk(students) }) {
                    Text("현재 학급 전체 발송")
                }
                Button(onClick = { viewModel.sendBulkAbsent(students) }) {
                    Text("결석 학생만 일괄 발송")
                }
            }

            StudentList(
                students = students,
                onMark = viewModel::mark,
                onSend = viewModel::sendToStudent,
                onDelete = viewModel::deleteStudent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassPicker(
    classes: List<SchoolClassEntity>,
    selected: SchoolClassEntity?,
    onSelected: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.grade} ${it.name}" } ?: "학급 선택",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text("학급") },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            classes.forEach { schoolClass ->
                DropdownMenuItem(
                    text = { Text("${schoolClass.grade} ${schoolClass.name}") },
                    onClick = {
                        onSelected(schoolClass.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun StudentList(
    students: List<StudentEntity>,
    onMark: (Long, AttendanceStatus) -> Unit,
    onSend: (StudentEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(students, key = { it.id }) { student ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("${student.name} (${student.parentPhone})")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { onMark(student.id, AttendanceStatus.PRESENT) }) { Text("출석") }
                    Button(onClick = { onMark(student.id, AttendanceStatus.ABSENT) }) { Text("결석") }
                    Button(onClick = { onMark(student.id, AttendanceStatus.LATE) }) { Text("지각") }
                    Button(onClick = { onSend(student) }) { Text("개별 문자") }
                    Button(onClick = { onDelete(student.id) }) { Text("삭제") }
                }
            }
        }
    }
}
