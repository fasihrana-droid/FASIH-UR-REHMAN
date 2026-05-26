package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceValueEntity
import com.example.data.DatabaseProvider
import com.example.data.GroupEntity
import com.example.data.MemberEntity
import com.example.data.AttendanceRepository
import com.example.data.SessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class MemberStats(
    val member: MemberEntity,
    val totalSessions: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val presenceRate: Float // 0.0 to 100.0
)

data class GroupStats(
    val totalMembers: Int,
    val totalSessions: Int,
    val overallPresenceRate: Float
)

class AttendanceViewModel(
    application: Application,
    private val repository: AttendanceRepository
) : AndroidViewModel(application) {

    // --- SELECTION STATES ---
    private val _selectedGroupId = MutableStateFlow<Int?>(null)
    val selectedGroupId: StateFlow<Int?> = _selectedGroupId.asStateFlow()

    private val _selectedSessionId = MutableStateFlow<Int?>(null)
    val selectedSessionId: StateFlow<Int?> = _selectedSessionId.asStateFlow()

    // --- REACTIVE FLOWS FROM DATABASE ---
    val allGroups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroup: StateFlow<GroupEntity?> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.allGroups.map { list -> list.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMembers: StateFlow<List<MemberEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMembersForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSessions: StateFlow<List<SessionEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getSessionsForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedSession: StateFlow<SessionEntity?> = _selectedSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else currentSessions.map { list -> list.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedSessionValues: StateFlow<List<AttendanceValueEntity>> = _selectedSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getAttendanceValuesForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All attendance values for the currently selected group (needed for stats and export)
    val currentGroupValues: StateFlow<List<AttendanceValueEntity>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getAllAttendanceValuesForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- REACTIVE STATISTICS COMBINATOR ---
    val memberStats: StateFlow<List<MemberStats>> = combine(
        currentMembers,
        currentSessions,
        currentGroupValues
    ) { members, sessions, values ->
        val sessionIds = sessions.map { it.id }.toSet()
        val valuesByMemberSpec = values.filter { it.sessionId in sessionIds }.groupBy { it.memberId }

        members.map { member ->
            val memberVals = valuesByMemberSpec[member.id] ?: emptyList()
            var present = 0
            var absent = 0
            var late = 0
            var excused = 0

            memberVals.forEach { valObj ->
                when (valObj.status) {
                    "PRESENT" -> present++
                    "ABSENT" -> absent++
                    "LATE" -> late++
                    "EXCUSED" -> excused++
                }
            }

            val markedCount = present + absent + late + excused
            val totalSessionsForMember = sessions.size
            
            // presence rate weights PRESENT = 1.0, LATE = 0.5 (or full present, let's treat LATE as 1.0 or 0.5, usually 1.0 or 0.5. Let's make LATE count as 1.0 flag but display separate count, or let's say: presenceRate = (Present + Late * 0.5) / sessions.size)
            val presenceRate = if (totalSessionsForMember > 0) {
                ((present.toFloat() + (late.toFloat() * 0.5f)) / totalSessionsForMember.toFloat()) * 100f
            } else {
                100f // default to 100% if no sessions exist
            }

            MemberStats(
                member = member,
                totalSessions = totalSessionsForMember,
                presentCount = present,
                absentCount = absent,
                lateCount = late,
                excusedCount = excused,
                presenceRate = Math.min(100f, Math.max(0f, presenceRate))
            )
        }.sortedBy { it.member.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupStats: StateFlow<GroupStats?> = combine(
        currentMembers,
        currentSessions,
        memberStats
    ) { members, sessions, statsList ->
        if (members.isEmpty()) return@combine GroupStats(0, 0, 0f)

        val totalRateSum = statsList.sumOf { it.presenceRate.toDouble() }
        val overall = (totalRateSum / members.size).toFloat()

        GroupStats(
            totalMembers = members.size,
            totalSessions = sessions.size,
            overallPresenceRate = overall
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    // --- BASE ACTIONS ---

    fun selectGroup(groupId: Int?) {
        _selectedGroupId.value = groupId
        _selectedSessionId.value = null // reset session when group changes
    }

    fun selectSession(sessionId: Int?) {
        _selectedSessionId.value = sessionId
    }

    fun createGroup(name: String, description: String) {
        viewModelScope.launch {
            val id = repository.createGroup(name, description)
            selectGroup(id.toInt())
        }
    }

    fun deleteCurrentGroup() {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch {
            repository.deleteGroupCascading(groupId)
            selectGroup(null)
        }
    }

    fun addMember(name: String, identifier: String) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch {
            repository.addMember(groupId, name, identifier)
        }
    }

    fun deleteMember(memberId: Int) {
        viewModelScope.launch {
            repository.deleteMemberCascading(memberId)
        }
    }

    fun createSession(title: String, date: Long) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch {
            val sessionId = repository.createSession(groupId, title, date)
            selectSession(sessionId.toInt())
        }
    }

    fun deleteCurrentSession() {
        val sessionId = _selectedSessionId.value ?: return
        viewModelScope.launch {
            repository.deleteSessionCascading(sessionId)
            selectSession(null)
        }
    }

    fun updateAttendance(memberId: Int, status: String, notes: String = "") {
        val sessionId = _selectedSessionId.value ?: return
        viewModelScope.launch {
            repository.updateAttendanceStatus(sessionId, memberId, status, notes)
        }
    }

    // Mark ALL as present or absent for quick setup
    fun setAllStatus(status: String) {
        val sessionId = _selectedSessionId.value ?: return
        val members = currentMembers.value
        viewModelScope.launch {
            val list = members.map { member ->
                AttendanceValueEntity(
                    sessionId = sessionId,
                    memberId = member.id,
                    status = status,
                    notes = ""
                )
            }
            repository.saveAllAttendanceValues(list)
        }
    }

    // CSV Exporter Format
    fun generateCSVString(): String {
        val group = selectedGroup.value ?: return ""
        val members = currentMembers.value
        val sessions = currentSessions.value.sortedBy { it.dateLong } // chronological
        val values = currentGroupValues.value

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val csv = StringBuilder()

        // Headers: Name, Roll/ID, Attendance %, Session Date 1, Session Date 2, ...
        csv.append("Name,Identifier/Roll,Attendance Rate %,")
        sessions.forEach { sess ->
            val formattedDate = sdf.format(Date(sess.dateLong))
            // escape double quotes
            val titleEscaped = sess.title.replace("\"", "\"\"")
            csv.append("\"$titleEscaped ($formattedDate)\",")
        }
        if (sessions.isNotEmpty()) {
            csv.deleteCharAt(csv.length - 1) // remove trailing comma
        }
        csv.append("\n")

        // Rows for each member
        val valuesBySessionAndMember = values.associateBy { Pair(it.sessionId, it.memberId) }
        val mStats = memberStats.value.associateBy { it.member.id }

        members.forEach { m ->
            val mName = m.name.replace("\"", "\"\"")
            val idStr = m.identifier.replace("\"", "\"\"")
            val rate = String.format(Locale.getDefault(), "%.1f%%", mStats[m.id]?.presenceRate ?: 100f)

            csv.append("\"$mName\",\"$idStr\",$rate,")

            sessions.forEach { s ->
                val valObj = valuesBySessionAndMember[Pair(s.id, m.id)]
                val statusStr = valObj?.status ?: "N/A"
                csv.append("\"$statusStr\",")
            }
            if (sessions.isNotEmpty()) {
                csv.deleteCharAt(csv.length - 1)
            }
            csv.append("\n")
        }

        return csv.toString()
    }
}

class AttendanceViewModelFactory(
    private val application: Application,
    private val repository: AttendanceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
