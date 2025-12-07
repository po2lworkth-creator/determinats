package com.example.wastesortmock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.wastesortmock.navigation.AppNavGraph
import com.example.wastesortmock.ui.theme.WasteTheme

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    
    WasteTheme {
        Scaffold { padding ->
            AppNavGraph(
                navController = navController,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

