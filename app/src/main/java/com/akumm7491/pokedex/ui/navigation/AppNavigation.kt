package com.akumm7491.pokedex.ui.navigation

import androidx.compose.animation.* // Import for animations
import androidx.compose.animation.core.tween // For animation duration/easing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.akumm7491.pokedex.ui.screens.PokemonDetailScreen
import com.akumm7491.pokedex.ui.screens.PokemonListScreen

// Sealed class for type-safe navigation routes
sealed class Screen(val route: String) {
    data object PokemonList : Screen("pokemon_list")
    data class PokemonDetail(val pokemonId: Int? = null) : Screen("pokemon_detail/{pokemonId}") {
        // Helper to create the route with arguments
        fun createRoute(id: Int) = "pokemon_detail/$id"
        // Argument name constant
        companion object { const val ARG_POKEMON_ID = "pokemonId" }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.PokemonList.route,
        modifier = modifier
    ) {
        // List Screen Composable
        composable(
            route = Screen.PokemonList.route,
            // Optional: Add animations when entering/exiting list screen
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            PokemonListScreen(
                // Pass navigation lambda to the list screen
                onPokemonClick = { pokemonId ->
                    navController.navigate(Screen.PokemonDetail().createRoute(pokemonId)){
                        launchSingleTop = true // Only allow for 1 detail screen at a time
                    }
                }
            )
        }

        // Detail Screen Composable
        composable(
            route = Screen.PokemonDetail().route, // Route with argument placeholder
            arguments = listOf(navArgument(Screen.PokemonDetail.ARG_POKEMON_ID) { type = NavType.IntType }),
            // Animations for navigating TO the detail screen
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            },
            // Animation for navigating BACK FROM the detail screen (pop)
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeOut(animationSpec = tween(400))
            },
            // Optional: Control animations when navigating AWAY from detail or returning TO it
            // exitTransition = { ... },
            // popEnterTransition = { ... }
        ) { navBackStackEntry ->
            // No need to manually extract argument if using Hilt ViewModel with SavedStateHandle
            PokemonDetailScreen(
                // Pass lambda to handle back navigation
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}