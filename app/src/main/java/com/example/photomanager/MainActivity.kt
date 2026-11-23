package com.example.photomanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photomanager.ui.PhotoManagerApp
import com.example.photomanager.ui.theme.PhotoManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: PhotoViewModel = viewModel()
            val darkTheme = when (vm.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PhotoManagerTheme(darkTheme = darkTheme) {
                PhotoManagerApp(vm = vm)
            }
        }
    }
}
