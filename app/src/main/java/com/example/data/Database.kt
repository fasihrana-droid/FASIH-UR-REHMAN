package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // --- GROUPS ---
    @Query("SELECT * FROM groups ORDER BY timestamp DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Int): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: Int)


    // --- MEMBERS ---
    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY name ASC")
    fun getMembersForGroup(groupId: Int): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY name ASC")
    suspend fun getMembersForGroupSync(groupId: Int): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMember(id: Int)

    @Query("DELETE FROM members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: Int)


    // --- SESSIONS ---
    @Query("SELECT * FROM sessions WHERE groupId = :groupId ORDER BY dateLong DESC, timestamp DESC")
    fun getSessionsForGroup(groupId: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Int)

    @Query("DELETE FROM sessions WHERE groupId = :groupId")
    suspend fun deleteSessionsForGroup(groupId: Int)


    // --- ATTENDANCE VALUES ---
    @Query("SELECT * FROM attendance_values WHERE sessionId = :sessionId")
    fun getAttendanceValuesForSession(sessionId: Int): Flow<List<AttendanceValueEntity>>

    @Query("SELECT * FROM attendance_values WHERE sessionId = :sessionId")
    suspend fun getAttendanceValuesForSessionSync(sessionId: Int): List<AttendanceValueEntity>

    @Query("SELECT av.* FROM attendance_values av INNER JOIN sessions s ON av.sessionId = s.id WHERE s.groupId = :groupId")
    fun getAllAttendanceValuesForGroup(groupId: Int): Flow<List<AttendanceValueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceValue(value: AttendanceValueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceValues(values: List<AttendanceValueEntity>)

    @Query("DELETE FROM attendance_values WHERE sessionId = :sessionId")
    suspend fun deleteAttendanceValuesForSession(sessionId: Int)

    @Query("DELETE FROM attendance_values WHERE memberId = :memberId")
    suspend fun deleteAttendanceValuesForMember(memberId: Int)

    @Query("DELETE FROM attendance_values WHERE sessionId IN (SELECT id FROM sessions WHERE groupId = :groupId)")
    suspend fun deleteAttendanceValuesForGroup(groupId: Int)
}

@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        SessionEntity::class,
        AttendanceValueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}
