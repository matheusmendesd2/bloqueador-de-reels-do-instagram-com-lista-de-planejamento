package com.rendox.routineblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rendox.routinetracker.core.ui.theme.RoutineTrackerTheme
import com.rendox.routineblocker.navigation.RoutineBlockerNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // O app tem identidade visual propria, entao o dynamic color fica desligado.
            RoutineTrackerTheme(disableDynamicColor = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RoutineBlockerNavHost()
                }
            }
        }
    }
}
