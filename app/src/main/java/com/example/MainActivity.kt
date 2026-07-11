package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MarketClockScreen
import com.example.ui.SettingsScreen
import com.example.ui.WorldClocksScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MarketViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: MarketViewModel = viewModel()
      val uiState by viewModel.uiState.collectAsState()

      MyApplicationTheme(darkTheme = uiState.darkTheme) {
        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(), // Ensures no overlapping with status bar, cutout, navigation gesture pill
          bottomBar = {
            NavigationBar(
              containerColor = if (uiState.darkTheme) Color(0xFF1E1F22) else Color.White,
              contentColor = if (uiState.darkTheme) Color.White else Color.Black
            ) {
              val activeColor = Color(0xFF34C759) // Premium green
              val inactiveColor = Color.Gray

              NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = {
                  Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Sessions"
                  )
                },
                label = { Text("Sessions") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = activeColor,
                  selectedTextColor = activeColor,
                  unselectedIconColor = inactiveColor,
                  unselectedTextColor = inactiveColor,
                  indicatorColor = Color.Transparent
                )
              )

              NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = {
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "World Clocks"
                  )
                },
                label = { Text("World Clocks") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = activeColor,
                  selectedTextColor = activeColor,
                  unselectedIconColor = inactiveColor,
                  unselectedTextColor = inactiveColor,
                  indicatorColor = Color.Transparent
                )
              )

              NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = {
                  Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                  )
                },
                label = { Text("Settings") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = activeColor,
                  selectedTextColor = activeColor,
                  unselectedIconColor = inactiveColor,
                  unselectedTextColor = inactiveColor,
                  indicatorColor = Color.Transparent
                )
              )
            }
          }
        ) { innerPadding ->
          when (selectedTab) {
            0 -> MarketClockScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )
            1 -> WorldClocksScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )
            2 -> SettingsScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    }
  }
}
