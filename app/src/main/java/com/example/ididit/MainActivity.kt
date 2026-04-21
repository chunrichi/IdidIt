package com.example.ididit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ididit.data.local.AppDatabase
import com.example.ididit.ui.navigation.AppNavigation
import com.example.ididit.ui.theme.IdidItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)

        setContent {
            IdidItTheme {
                AppNavigation(database = database)
            }
        }
    }
}
