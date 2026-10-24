package com.bananacreat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bananacreat.ui.gallery.GalleryScreen
import com.bananacreat.ui.canvas.CanvasScreen
import com.bananacreat.ui.theme.BananaCreatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BananaCreatApp()
        }
    }
}

@Composable
fun BananaCreatApp() {
    BananaCreatTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val canvasViewModel: CanvasViewModel = viewModel()

            NavHost(navController = navController, startDestination = "gallery") {
                composable("gallery") {
                    GalleryScreen(
                        viewModel = canvasViewModel,
                        navController = navController
                    )
                }
                composable("canvas") {
                    CanvasScreen(
                        viewModel = canvasViewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}
