package com.akumm7491.pokedex.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.akumm7491.pokedex.ui.screens.PokemonDetailScreen
import com.akumm7491.pokedex.ui.screens.PokemonListScreen

sealed class Screen(val route: String) {
    data object PokemonList : Screen("pokemon_list")
    data class PokemonDetail(val pokemonId: Int? = null) : Screen("pokemon_detail/{pokemonId}") {
        fun createRoute(id: Int) = "pokemon_detail/$id"
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
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            PokemonListScreen(
                onPokemonClick = { pokemonId ->
                    navController.navigate(Screen.PokemonDetail().createRoute(pokemonId)){
                        launchSingleTop = true // Only allow for 1 detail screen at a time
                    }
                }
            )
        }

        // Detail Screen Composable
        composable(
            route = Screen.PokemonDetail().route,
            arguments = listOf(navArgument(Screen.PokemonDetail.ARG_POKEMON_ID) { type = NavType.IntType }),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) +
                        fadeOut(animationSpec = tween(400))
            },
        ) { _ ->
            PokemonDetailScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}