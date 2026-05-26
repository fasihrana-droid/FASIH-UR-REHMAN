package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MemberEntity
import com.example.data.GroupEntity
import com.example.data.SessionEntity
import com.example.data.AttendanceValueEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AttendanceViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe streams
    val allGroups by viewModel.allGroups.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val currentMembers by viewModel.currentMembers.collectAsStateWithLifecycle()
    val currentSessions by viewModel.currentSessions.collectAsStateWithLifecycle()
    val selectedSession by viewModel.selectedSession.collectAsStateWithLifecycle()
    val selectedSessionValues by viewModel.selectedSessionValues.collectAsStateWithLifecycle()
    val memberStats by viewModel.memberStats.collectAsStateWithLifecycle()
    val groupStats by viewModel.groupStats.collectAsStateWithLifecycle()

    // Dialog & UI interactive states
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showDeleteGroupConfirm by remember { mutableStateOf(false) }
    var showDeleteSessionConfirm by remember { mutableStateOf(false) }
    val memberToDelete = remember { mutableStateOf<MemberEntity?>(null) }
    
    // Internal group tabs: 0 = Sessions, 1 = Members, 2 = Analytics/Stats
    var activeTabSubIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedGroup?.name ?: "Attendance Record Maker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedGroup != null && selectedGroup?.description?.isNotEmpty() == true) {
                            Text(
                                text = selectedGroup!!.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (selectedGroup != null) {
                        IconButton(
                            onClick = {
                                if (selectedSession != null) {
                                    viewModel.selectSession(null)
                                } else {
                                    viewModel.selectGroup(null)
                                }
                            },
                            modifier = Modifier.testTag("back_button_navigation")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (selectedGroup != null && selectedSession == null) {
                        IconButton(
                            onClick = {
                                val csv = viewModel.generateCSVString()
                                if (csv.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(csv))
                                    Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("export_csv_action")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Export Excel/CSV")
                        }
                        IconButton(
                            onClick = { showDeleteGroupConfirm = true },
                            modifier = Modifier.testTag("delete_group_header_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Group",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (selectedSession != null) {
                        IconButton(
                            onClick = { showDeleteSessionConfirm = true },
                            modifier = Modifier.testTag("delete_session_header_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Session",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            // Context aware Floating Action Button (FAB)
            if (selectedGroupId == null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("New Group") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("create_group_fab")
                )
            } else if (selectedSession == null) {
                // Inside a group, showFAB based on chosen tab
                when (activeTabSubIndex) {
                    0 -> { // Sessions Tab
                        ExtendedFloatingActionButton(
                            onClick = { showCreateSessionDialog = true },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Add") },
                            text = { Text("New Session") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("create_session_fab")
                        )
                    }
                    1 -> { // Members Tab
                        ExtendedFloatingActionButton(
                            onClick = { showAddMemberDialog = true },
                            icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add") },
                            text = { Text("Add Member") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("add_member_fab")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                selectedGroupId == null -> {
                    // 1. Group List/Dashboard Screen
                    GroupSelectorView(
                        groups = allGroups,
                        onGroupClick = { viewModel.selectGroup(it.id) },
                        onCreateClick = { showCreateGroupDialog = true }
                    )
                }
                selectedSession != null -> {
                    // 2. Attendance Recording Dashboard
                    AttendanceRecordingView(
                        session = selectedSession!!,
                        members = currentMembers,
                        values = selectedSessionValues,
                        onStatusChange = { memberId, status ->
                            viewModel.updateAttendance(memberId, status)
                        },
                        onMarkAll = { status ->
                            viewModel.setAllStatus(status)
                        }
                    )
                }
                else -> {
                    // 3. Tabbed Group Explorer (Sessions, Members, Stats)
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = activeTabSubIndex,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = activeTabSubIndex == 0,
                                onClick = { activeTabSubIndex = 0 },
                                text = { Text("Sessions", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Schedule, contentDescription = null) }
                            )
                            Tab(
                                selected = activeTabSubIndex == 1,
                                onClick = { activeTabSubIndex = 1 },
                                text = { Text("Members", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.People, contentDescription = null) }
                            )
                            Tab(
                                selected = activeTabSubIndex == 2,
                                onClick = { activeTabSubIndex = 2 },
                                text = { Text("Statistics", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (activeTabSubIndex) {
                                0 -> SessionsTab(
                                    sessions = currentSessions,
                                    onSessionSelected = { viewModel.selectSession(it.id) },
                                    onNewSessionClick = { showCreateSessionDialog = true }
                                )
                                1 -> MembersTab(
                                    members = currentMembers,
                                    onDeleteMember = { memberToDelete.value = it }
                                )
                                2 -> StatisticsTab(
                                    groupStats = groupStats,
                                    memberStats = memberStats,
                                    onExportCsv = {
                                        val csv = viewModel.generateCSVString()
                                        if (csv.isNotEmpty()) {
                                            clipboardManager.setText(AnnotatedString(csv))
                                            Toast.makeText(context, "Entire Group CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS SECTION ---

    // 1. Create Group Dialog
    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create New Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {
                            groupName = it
                            error = null
                        },
                        label = { Text("Group Name *") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input")
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.trim().isEmpty()) {
                            error = "Group name is required."
                        } else {
                            viewModel.createGroup(groupName.trim(), groupDesc.trim())
                            showCreateGroupDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_group_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Create Session Dialog
    if (showCreateSessionDialog) {
        var sessionTitle by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val defaultTitle = "Session " + sdf.format(Date())

        LaunchedEffect(Unit) {
            sessionTitle = defaultTitle
        }

        AlertDialog(
            onDismissRequest = { showCreateSessionDialog = false },
            title = { Text("Add Attendance Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Set a date label or name for this recording session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = sessionTitle,
                        onValueChange = {
                            sessionTitle = it
                            error = null
                        },
                        label = { Text("Session Name / Date *") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_title_input")
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sessionTitle.trim().isEmpty()) {
                            error = "Session title cannot be empty."
                        } else {
                            viewModel.createSession(sessionTitle.trim(), System.currentTimeMillis())
                            showCreateSessionDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_session_button")
                ) {
                    Text("Add & Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Add Member Dialog
    if (showAddMemberDialog) {
        var memberName by remember { mutableStateOf("") }
        var rollNumber by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Group Member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {
                            memberName = it
                            error = null
                        },
                        label = { Text("Member Name *") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_name_input")
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll No. / ID (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_id_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memberName.trim().isEmpty()) {
                            error = "Member Name is required."
                        } else {
                            viewModel.addMember(memberName.trim(), rollNumber.trim())
                            showAddMemberDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_member_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Delete Group Confirmation
    if (showDeleteGroupConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm = false },
            title = { Text("Delete Entire Group?") },
            text = {
                Text("This will permanently purge this group, all of its recorded members, sessions, and historic attendance data. This action is critical and cannot be undone.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteCurrentGroup()
                        showDeleteGroupConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_delete_group")
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Delete Session Confirmation
    if (showDeleteSessionConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSessionConfirm = false },
            title = { Text("Delete This Session?") },
            text = {
                Text("This will permanently delete this attendance session recording. Data cannot be recovered.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteCurrentSession()
                        showDeleteSessionConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_delete_session")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSessionConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Delete Member Confirmation
    val currentMemberToDelete = memberToDelete.value
    if (currentMemberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete.value = null },
            title = { Text("Remove Member?") },
            text = {
                Text("This will permanently remove ${currentMemberToDelete.name} and all their attendance records from this group.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteMember(currentMemberToDelete.id)
                        memberToDelete.value = null
                    },
                    modifier = Modifier.testTag("confirm_delete_member")
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete.value = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SUB-VIEW COMPONENTS
// -------------------------------------------------------------

@Composable
fun GroupSelectorView(
    groups: List<com.example.data.GroupEntity>,
    onGroupClick: (com.example.data.GroupEntity) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        VerticalEmptyIllustrationHeader(
            title = "Attendance Manager",
            subtitle = "Keep crisp, complete records of students, corporate teams, classes, and shifts."
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    onClick = onCreateClick,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .testTag("empty_group_card_trigger"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No groups yet",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create your first class, event, or team group to start tracking attendance safely.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = onCreateClick) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Group")
                        }
                    }
                }
            }
        } else {
            Text(
                text = "My Groups (${groups.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(groups, key = { it.id }) { group ->
                    Card(
                        onClick = { onGroupClick(group) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .testTag("group_card_${group.id}"),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (group.description.isNotEmpty()) {
                                    Text(
                                        text = group.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalEmptyIllustrationHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    )
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AssignmentTurnedIn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- SUB-TABS OF GROUP ---

@Composable
fun SessionsTab(
    sessions: List<com.example.data.SessionEntity>,
    onSessionSelected: (com.example.data.SessionEntity) -> Unit,
    onNewSessionClick: () -> Unit
) {
    if (sessions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No sessions recorded yet",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Each day, lecture, or shift can be recorded as a unique session. Tap '+' below to create one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNewSessionClick) {
                Text("Start Attendance")
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sessions, key = { it.id }) { session ->
                val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                val formattedDate = sdf.format(Date(session.dateLong))
                
                Card(
                    onClick = { onSessionSelected(session) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("session_card_${session.id}"),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = session.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open session",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MembersTab(
    members: List<com.example.data.MemberEntity>,
    onDeleteMember: (com.example.data.MemberEntity) -> Unit
) {
    if (members.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PeopleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No members in this group",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add students, staff, or members into this group first so that they will be listed during attendance sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(members, key = { it.id }) { member ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("member_card_${member.id}"),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (member.identifier.isNotEmpty()) {
                                    Text(
                                        text = "ID: ${member.identifier}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { onDeleteMember(member) },
                            modifier = Modifier.testTag("delete_member_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonRemove,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsTab(
    groupStats: GroupStats?,
    memberStats: List<MemberStats>,
    onExportCsv: () -> Unit
) {
    if (memberStats.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No analytics available yet",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Analytics and attendance metrics will display here after you add members and create sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Overall Stats Card
            if (groupStats != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Group Metric Summary",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatBanner(
                                    label = "Overall Presence",
                                    value = String.format(Locale.getDefault(), "%.1f%%", groupStats.overallPresenceRate),
                                    modifier = Modifier.weight(1f)
                                )
                                StatBanner(
                                    label = "Total Members",
                                    value = "${groupStats.totalMembers}",
                                    modifier = Modifier.weight(1f)
                                )
                                StatBanner(
                                    label = "Sessions",
                                    value = "${groupStats.totalSessions}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onExportCsv,
                                modifier = Modifier.fillMaxWidth().testTag("export_csv_btn_stats")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Grid to Clipboard (CSV)")
                            }
                        }
                    }
                }
            }

            // Member Breakdown Headers
            item {
                Text(
                    text = "Leaderboard & Attendance Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Members lists
            items(memberStats, key = { it.member.id }) { stat ->
                val rateColor = when {
                    stat.presenceRate >= 85f -> Color(0xFF2E7D32) // green
                    stat.presenceRate >= 70f -> Color(0xFFE65100) // orange
                    else -> Color(0xFFC62828) // red
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stat.member.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (stat.member.identifier.isNotEmpty()) {
                                    Text(
                                        text = "ID: ${stat.member.identifier}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Custom Percentage Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(rateColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", stat.presenceRate),
                                    color = rateColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Mini stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniBadge(label = "Present", count = stat.presentCount, color = Color(0xFF2E7D32))
                            MiniBadge(label = "Absent", count = stat.absentCount, color = Color(0xFFC62828))
                            MiniBadge(label = "Late", count = stat.lateCount, color = Color(0xFFEF6C00))
                            MiniBadge(label = "Excused", count = stat.excusedCount, color = Color(0xFF6A1B9A))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Indicator
                        LinearProgressIndicator(
                            progress = { stat.presenceRate / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = rateColor,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBanner(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MiniBadge(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $count",
            fontSize = 11.sp,
            color = if (MaterialTheme.colorScheme.background == Color.White) color else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

// -------------------------------------------------------------
// LIVE ATTENDANCE RECORDING VIEW PANEL
// -------------------------------------------------------------

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AttendanceRecordingView(
    session: com.example.data.SessionEntity,
    members: List<com.example.data.MemberEntity>,
    values: List<com.example.data.AttendanceValueEntity>,
    onStatusChange: (memberId: Int, status: String) -> Unit,
    onMarkAll: (status: String) -> Unit
) {
    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    val formattedDate = sdf.format(Date(session.dateLong))

    // Calculate dynamic stats
    val totalCount = members.size
    val valuesByMember = values.associateBy { it.memberId }

    val presentCount = values.count { it.status == "PRESENT" }
    val absentCount = values.count { it.status == "ABSENT" }
    val lateCount = values.count { it.status == "LATE" }
    val excusedCount = values.count { it.status == "EXCUSED" }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredMembers = remember(members, values, selectedFilter) {
        if (selectedFilter == "ALL") {
            members
        } else {
            members.filter { m ->
                val valObj = valuesByMember[m.id]
                val status = valObj?.status ?: "PRESENT"
                status == selectedFilter
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // High Density Summary / Date Badge Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = session.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$formattedDate • $totalCount enrolled",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$presentCount",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF006E1C)
                        )
                        Text(
                            text = "PRES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$absentCount",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFFBA1A1A)
                        )
                        Text(
                            text = "ABS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChipItem(
                label = "All",
                isSelected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" }
            )
            FilterChipItem(
                label = "Present",
                isSelected = selectedFilter == "PRESENT",
                onClick = { selectedFilter = "PRESENT" }
            )
            FilterChipItem(
                label = "Absent",
                isSelected = selectedFilter == "ABSENT",
                onClick = { selectedFilter = "ABSENT" }
            )
            FilterChipItem(
                label = "Late",
                isSelected = selectedFilter == "LATE",
                onClick = { selectedFilter = "LATE" }
            )
            FilterChipItem(
                label = "Excused",
                isSelected = selectedFilter == "EXCUSED",
                onClick = { selectedFilter = "EXCUSED" }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Quick bulk actions ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bulk Actions:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onMarkAll("PRESENT") },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("bulk_present_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("All Present", fontSize = 11.sp)
                }
                TextButton(
                    onClick = { onMarkAll("ABSENT") },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("bulk_absent_btn"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("All Absent", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Lazy Column loaded with custom interactive status widgets
        if (filteredMembers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedFilter == "ALL") "No members in this group." else "No members with status: $selectedFilter",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredMembers, key = { it.id }) { member ->
                    val attendanceVal = valuesByMember[member.id]
                    val currentStatus = attendanceVal?.status ?: "PRESENT"

                    AttendanceItemCard(
                        member = member,
                        status = currentStatus,
                        onStatusSelected = { newStatus ->
                            onStatusChange(member.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PillCounter(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: $count",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SegmentedStatusControl(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedItem(
            text = "P",
            isSelected = selectedStatus == "PRESENT",
            selectedBg = Color(0xFF005FB0), // High density blue-purple for Present
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { onStatusSelected("PRESENT") }
        )
        
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
        
        SegmentedItem(
            text = "A",
            isSelected = selectedStatus == "ABSENT",
            selectedBg = Color(0xFFBA1A1A), // High density red for Absent
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { onStatusSelected("ABSENT") }
        )
        
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
        
        SegmentedItem(
            text = "L",
            isSelected = selectedStatus == "LATE",
            selectedBg = Color(0xFF605D64), // Late slate color
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { onStatusSelected("LATE") }
        )
        
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
        
        SegmentedItem(
            text = "E",
            isSelected = selectedStatus == "EXCUSED",
            selectedBg = Color(0xFF6A1B9A), // Excused purple
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { onStatusSelected("EXCUSED") }
        )
    }
}

@Composable
fun RowScope.SegmentedItem(
    text: String,
    isSelected: Boolean,
    selectedBg: Color,
    unselectedTextColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (isSelected) selectedBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else unselectedTextColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun AttendanceItemCard(
    member: com.example.data.MemberEntity,
    status: String,
    onStatusSelected: (String) -> Unit
) {
    val hash = Math.abs(member.name.hashCode())
    val (avatarBg, avatarText) = when (hash % 3) {
        0 -> Pair(Color(0xFFDEE1F9), Color(0xFF161B2C))
        1 -> Pair(Color(0xFFF9DEDC), Color(0xFF410E0B))
        else -> Pair(Color(0xFFE1E2EC), Color(0xFF1A1C1E))
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_item_${member.id}"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Initial Circle Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(avatarBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = member.name.firstOrNull()?.uppercase() ?: "?"
                    Text(
                        text = initial,
                        color = avatarText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Column {
                    Text(
                        text = member.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (member.identifier.isNotEmpty()) {
                        Text(
                            text = "ID: ${member.identifier}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // High density segmented control
            SegmentedStatusControl(
                selectedStatus = status,
                onStatusSelected = onStatusSelected
            )
        }
    }
}
