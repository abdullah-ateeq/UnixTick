package com.example.unixtick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.unixtick.data.storage.UnixTickPreferences
import com.example.unixtick.ui.navigation.UnixTickNavHost
import com.example.unixtick.ui.theme.UnixTickTheme
import com.example.unixtick.ui.viewmodel.UnixTickViewModel
import com.example.unixtick.ui.viewmodel.UnixTickViewModelFactory

class MainActivity : ComponentActivity() {

    private val preferences by lazy {
        UnixTickPreferences(applicationContext)
    }

    private val viewModel: UnixTickViewModel by viewModels {
        UnixTickViewModelFactory(preferences)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()

            UnixTickTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UnixTickNavHost(viewModel = viewModel)
                }
            }
        }
    }
}