package com.example.wastesortmock.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.wastesortmock.ui.screens.BarcodeScreen
import com.example.wastesortmock.ui.screens.CameraScreen
import com.example.wastesortmock.ui.screens.CategoriesScreen
import com.example.wastesortmock.ui.screens.CategoryDetailsScreen
import com.example.wastesortmock.ui.screens.HomeScreen
import com.example.wastesortmock.ui.screens.ProductSearchScreen
import com.example.wastesortmock.ui.screens.StatsScreen
import com.example.wastesortmock.ui.screens.ResultScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToFrontCamera = {
                    navController.navigate("camera")
                },
                onNavigateToBarcode = {
                    navController.navigate("barcode")
                },
                onNavigateToItem = {
                    navController.navigate("product")
                },
                onNavigateToStats = {
                    navController.navigate("stats")
                },
                onNavigateToCategories = {
                    navController.navigate("categories")
                }
            )
        }
        
        composable("barcode") {
            BarcodeScreen()
        }
        
        composable("product") {
            ProductSearchScreen(
                onNavigateToResult = { categoryId ->
                    navController.navigate("result/$categoryId")
                }
            )
        }
        
        composable("categories") {
            CategoriesScreen(
                onNavigateToCategory = { categoryId ->
                    navController.navigate("category/$categoryId")
                }
            )
        }
        
        composable(
            route = "category/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("id") ?: ""
            CategoryDetailsScreen(
                categoryId = categoryId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("camera") {
            CameraScreen(
                useFrontCamera = false,
                onNavigateToResult = { categoryId ->
                    navController.navigate("result/$categoryId")
                }
            )
        }

        composable("stats") {
            StatsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "result/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("id") ?: ""
            ResultScreen(
                categoryId = categoryId,
                onNavigateToCamera = {
                    navController.navigate("camera") {
                        popUpTo("camera") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}

