package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AttendanceRepository
import com.example.data.DatabaseProvider
import com.example.ui.AttendanceViewModel
import com.example.ui.AttendanceViewModelFactory
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room DB components
        val database = DatabaseProvider.getDatabase(this)
        val repository = AttendanceRepository(database.attendanceDao())
        
        // Instantiate our ViewModel
        val factory = AttendanceViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[AttendanceViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
