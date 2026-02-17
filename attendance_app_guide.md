# 방과후 출결 관리 안드로이드 앱 개발 가이드

## 0) 먼저 결정할 것(권장 기술 스택)
- **언어/플랫폼**: Kotlin + Android
- **UI**: Jetpack Compose(권장) 또는 XML
- **로컬 DB**: Room(SQLite)
- **아키텍처**: MVVM (ViewModel + Repository)
- **주소록 읽기**: Contacts Provider (읽기 전용)
- **문자 발송**:
  - 즉시 발송: `SmsManager` + `SEND_SMS` 권한
  - 기본 문자앱 열기(권한 부담 ↓): `ACTION_SENDTO` (`smsto:`)

> 핵심: **휴대전화 주소록은 읽기만 하고, 앱 내부 DB에 학생/반 정보를 따로 저장**합니다.

---

## 1) 요구사항을 데이터 모델로 분해하기
요구사항을 그대로 테이블로 만들면 구현이 단순해집니다.

### 1-1. 엔티티 설계(Room)
1. `SchoolClass`(학급)
   - `id`, `name`, `grade`, `description`
2. `Student`(학생)
   - `id`, `classId(FK)`, `name`, `parentPhone`, `sourceContactId(nullable)`
   - `sourceContactId`는 "어느 주소록 항목에서 가져왔는지" 추적용
3. `Attendance`(출결)
   - `id`, `classId`, `studentId`, `date`, `status(PRESENT/ABSENT/LATE)`, `memo`
4. `MessageTemplate`(문자 템플릿)
   - `id`, `name`, `body`
   - 예: "학생이 출결하지 않았습니다."

### 1-2. 핵심 포인트
- 학생 추가/삭제는 **앱 DB(Student 테이블)** 에서만 수행.
- 주소록은 가져올 때만 읽고, 주소록 수정/삭제 API는 사용하지 않음.

---

## 2) 프로젝트 생성 및 권한 설정

### 2-1. AndroidManifest 권한
- 주소록 읽기: `READ_CONTACTS`
- 문자 발송(직접 발송 방식 선택 시): `SEND_SMS`

```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.SEND_SMS" />
```

### 2-2. 런타임 권한 처리
- 앱 시작 시가 아니라, 해당 기능 진입 시 요청(학급 학생 가져오기/문자 발송 시점).
- 거부 시 대체 경로 제공:
  - 주소록 권한 거부 → "직접 학생 등록" 화면
  - SMS 권한 거부 → 기본 문자 앱 열기 방식으로 안내

---

## 3) 주소록에서 학생 가져오기(단, 내부 DB 저장)

### 3-1. 주소록 조회
- `ContactsContract.CommonDataKinds.Phone`로 이름/전화번호 조회
- 검색 UI에서 선택한 항목만 `Student`로 insert

### 3-2. 중복 처리 규칙
- 같은 학급에서 `name + parentPhone` 중복 방지
- 전화번호는 저장 전에 정규화(공백/하이픈 제거)

### 3-3. 삭제
- 학생 삭제는 `Student` 레코드 삭제만 수행
- 주소록 삭제 API 호출 금지

---

## 4) 출결 체크 UI 구성

### 4-1. 화면 구조
1. 학급 목록 화면
2. 학급 상세(학생 목록 + 오늘 출결 토글)
3. 문자 발송 화면(템플릿/직접입력 + 일괄/개별)

### 4-2. 출결 체크 방식
- 학생 카드에 상태 선택(PRESENT/ABSENT/LATE)
- 저장 버튼 클릭 시 `Attendance(date=today)` upsert

---

## 5) 문자 메시지 조합 규칙 구현
요구사항의 문자열 규칙을 함수로 고정하세요.

```kotlin
fun buildMessage(studentName: String, body: String): String {
    return "${studentName}학생 $body".trim()
}
```

예)
- 학생이름: 길길
- 본문: 학생이 출결하지 않았습니다.
- 결과: `길길학생 학생이 출결하지 않았습니다.`

> 문구를 자연스럽게 하려면 띄어쓰기 규칙을 팀에서 확정하세요.
> 예: `"${studentName} 학생이 출결하지 않았습니다."`

---

## 6) 문자 발송 기능(일괄/개별)

### 6-1. 개별 발송
- 학생 1명 선택
- 템플릿 선택 또는 직접 입력
- `buildMessage(name, body)`로 최종 문자열 생성
- 해당 학부모 번호로 전송

### 6-2. 일괄 발송
- 학급 화면에서 오늘 출결 상태 필터
  - 예: `ABSENT`인 학생만 선택
- 선택 학생 목록 순회하며 개별 발송 로직 재사용

```kotlin
selectedStudents.forEach { student ->
    val msg = buildMessage(student.name, body)
    smsSender.send(student.parentPhone, msg)
}
```

### 6-3. 발송 방식 2가지
1. **자동 발송(SmsManager)**
   - 빠르지만 권한/정책/단말 제약 고려 필요
2. **기본 문자 앱 열기(Intent)**
   - 안정적, 사용자 확인 후 발송

```kotlin
val intent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("smsto:${phone}")
    putExtra("sms_body", message)
}
startActivity(intent)
```

---

## 7) 안정성/실무 체크리스트
1. **전화번호 유효성 검사**: 비어있거나 너무 짧은 번호 차단
2. **발송 로그 저장**: 누가/언제/무슨 메시지 발송했는지 DB 기록
3. **중복 발송 방지**: 동일 학생에게 같은 날 연속 발송 시 경고
4. **개인정보 보호**: DB 암호화(SQLCipher 고려), 백업 정책 확인
5. **권한 거부 대응 UX**: 설정 이동 안내

---

## 8) 추천 개발 순서(스프린트 방식)
1. **DB + 학급/학생 CRUD 먼저 완성**
2. 주소록 읽어와 학생 추가 기능 연결
3. 출결 저장/조회 기능 구현
4. 템플릿 + 메시지 조합 함수 구현
5. 개별 문자 발송 구현
6. 일괄 발송(선택/필터) 구현
7. 발송 로그/예외 처리/UX 개선
8. 실제 단말 테스트(권한/문자앱/SIM 유무)

---

## 9) 최소 동작 예시 유스케이스
1. "3학년 A반" 생성
2. 주소록에서 학생 10명 선택 후 추가
3. 오늘 출결 체크(2명 결석)
4. 템플릿 "학생이 출결하지 않았습니다." 선택
5. "결석 학생만 일괄 발송" 클릭
6. 각 학부모에게 `[이름]학생 ...` 규칙으로 발송

---

## 10) 다음 단계(원하면 바로 확장)
- 학부모 다중 번호(아버지/어머니) 지원
- 카카오 알림톡/푸시 대체 채널
- 월간 출결 통계/엑셀 내보내기
- 교사 계정 잠금(생체인증)

필요하시면 다음 답변에서
1) **Room 엔티티/DAO 전체 코드 템플릿**,  
2) **Compose 화면 샘플(학급/출결/발송)**,  
3) **문자 발송 모듈 인터페이스 설계**  
를 바로 실행 가능한 형태로 작성해드릴게요.
