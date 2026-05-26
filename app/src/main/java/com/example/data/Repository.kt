package com.example.data

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val dao: AttendanceDao) {

    // --- GROUPS ---
    val allGroups: Flow<List<GroupEntity>> = dao.getAllGroups()

    suspend fun getGroupById(id: Int): GroupEntity? = dao.getGroupById(id)

    suspend fun createGroup(name: String, description: String): Long {
        return dao.insertGroup(GroupEntity(name = name, description = description))
    }

    suspend fun deleteGroupCascading(groupId: Int) {
        // First, delete a group's attendance values, then sessions, then members, then the group itself.
        dao.deleteAttendanceValuesForGroup(groupId)
        dao.deleteSessionsForGroup(groupId)
        dao.deleteMembersForGroup(groupId)
        dao.deleteGroup(groupId)
    }


    // --- MEMBERS ---
    fun getMembersForGroup(groupId: Int): Flow<List<MemberEntity>> = dao.getMembersForGroup(groupId)

    suspend fun getMembersForGroupSync(groupId: Int): List<MemberEntity> = dao.getMembersForGroupSync(groupId)

    suspend fun addMember(groupId: Int, name: String, identifier: String): Long {
        return dao.insertMember(MemberEntity(groupId = groupId, name = name, identifier = identifier))
    }

    suspend fun deleteMemberCascading(memberId: Int) {
        dao.deleteAttendanceValuesForMember(memberId)
        dao.deleteMember(memberId)
    }


    // --- SESSIONS ---
    fun getSessionsForGroup(groupId: Int): Flow<List<SessionEntity>> = dao.getSessionsForGroup(groupId)

    suspend fun createSession(groupId: Int, title: String, dateLong: Long): Long {
        val sessionId = dao.insertSession(SessionEntity(groupId = groupId, title = title, dateLong = dateLong))
        
        // When we create a new session, auto-initialize all active members with a default status is handy,
        // or we can initialize them dynamically. Let's pre-populate them as "PRESENT" or empty so they can be individually modified.
        val members = dao.getMembersForGroupSync(groupId)
        val initialValues = members.map { member ->
            AttendanceValueEntity(
                sessionId = sessionId.toInt(),
                memberId = member.id,
                status = "PRESENT", // default to present so users can just mark "absent" easily
                notes = ""
            )
        }
        if (initialValues.isNotEmpty()) {
            dao.insertAttendanceValues(initialValues)
        }
        
        return sessionId
    }

    suspend fun deleteSessionCascading(sessionId: Int) {
        dao.deleteAttendanceValuesForSession(sessionId)
        dao.deleteSession(sessionId)
    }


    // --- ATTENDANCE VALUES ---
    fun getAttendanceValuesForSession(sessionId: Int): Flow<List<AttendanceValueEntity>> = 
        dao.getAttendanceValuesForSession(sessionId)

    suspend fun getAttendanceValuesForSessionSync(sessionId: Int): List<AttendanceValueEntity> = 
        dao.getAttendanceValuesForSessionSync(sessionId)

    fun getAllAttendanceValuesForGroup(groupId: Int): Flow<List<AttendanceValueEntity>> =
        dao.getAllAttendanceValuesForGroup(groupId)

    suspend fun updateAttendanceStatus(sessionId: Int, memberId: Int, status: String, notes: String = "") {
        // Find existing value or insert new
        val existing = dao.getAttendanceValuesForSessionSync(sessionId).find { it.memberId == memberId }
        val valueToSave = if (existing != null) {
            existing.copy(status = status, notes = notes)
        } else {
            AttendanceValueEntity(sessionId = sessionId, memberId = memberId, status = status, notes = notes)
        }
        dao.insertAttendanceValue(valueToSave)
    }

    suspend fun saveAllAttendanceValues(values: List<AttendanceValueEntity>) {
        dao.insertAttendanceValues(values)
    }
}
