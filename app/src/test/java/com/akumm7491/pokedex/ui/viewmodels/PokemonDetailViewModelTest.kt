package com.akumm7491.pokedex.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.akumm7491.pokedex.domain.models.PokemonDetailResponse
import com.akumm7491.pokedex.domain.repositories.PokemonRepository
import com.akumm7491.pokedex.ui.navigation.Screen
import com.akumm7491.pokedex.utils.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PokemonDetailViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    // Mocks
    private lateinit var repository: PokemonRepository
    private lateinit var savedStateHandle: SavedStateHandle

    // Class Under Test
    private lateinit var viewModel: PokemonDetailViewModel

    // Constants for testing
    private val testPokemonId = 25
    private val testPokemonName = "Pikachu"

    @Before
    fun setUp() {
        repository = mockk()
        savedStateHandle = mockk<SavedStateHandle>(relaxed = true).apply {
            // Mock SavedStateHandle to return our test ID
            every { get<Int>(Screen.PokemonDetail.ARG_POKEMON_ID) } returns testPokemonId
        }
    }

    // Helper to create a dummy detail response
    private fun createDummyDetailResponse(id: Int, name: String): PokemonDetailResponse {
        return PokemonDetailResponse(
            id = id,
            name = name,
            height = 4,
            weight = 60,
            types = emptyList(),
            stats = emptyList(),
            abilities = emptyList(),
            moves = emptyList(),
            sprites = mockk(relaxed = true)
        )
    }

    // --- Test Cases --- 

    @Test
    fun `fetch details success updates state correctly`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange
        val successResponse = createDummyDetailResponse(testPokemonId, testPokemonName)
        // Mock repository BEFORE ViewModel init triggers fetchDetails
        coEvery { repository.fetchPokemonDetail(testPokemonId.toString()) } returns Result.success(successResponse)

        // Initialize ViewModel here AFTER mocking the repo for the init call
        viewModel = PokemonDetailViewModel(savedStateHandle, repository)

        // Act & Assert
        viewModel.state.test {
            // 1. Initial state (isLoading=true from init block starting fetch)
            val initialState = awaitItem()
            assertTrue("Initial state should be loading", initialState.isLoading)
            assertNull("Initial state pokemon should be null", initialState.pokemon)
            assertNull("Initial state error should be null", initialState.error)

            // 2. Success state (after repository returns)
            val successState = awaitItem()
            assertFalse("Success state should not be loading", successState.isLoading)
            assertNotNull("Success state pokemon should not be null", successState.pokemon)
            assertEquals("Success state pokemon ID should match", testPokemonId, successState.pokemon?.id)
            assertEquals("Success state pokemon name should match", testPokemonName, successState.pokemon?.name)
            assertNull("Success state error should be null", successState.error)

            // Ensure no more state changes
            expectNoEvents()
        }

        // Verify repository call
        coVerify(exactly = 1) { repository.fetchPokemonDetail(testPokemonId.toString()) }
    }

    @Test
    fun `fetch details failure updates state correctly`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange
        val errorMessage = "Pokemon not found"
        val exception = RuntimeException(errorMessage)
        // Mock repository to return failure
        coEvery { repository.fetchPokemonDetail(testPokemonId.toString()) } returns Result.failure(exception)

        // Initialize ViewModel AFTER mocking
        viewModel = PokemonDetailViewModel(savedStateHandle, repository)

        // Act & Assert
        viewModel.state.test {
            // 1. Initial loading state
            val initialState = awaitItem()
            assertTrue("Initial state should be loading", initialState.isLoading)
            assertNull("Initial pokemon should be null", initialState.pokemon)
            assertNull("Initial error should be null", initialState.error)

            // 2. Failure state
            val failureState = awaitItem()
            assertFalse("Failure state should not be loading", failureState.isLoading)
            assertNull("Failure state pokemon should be null", failureState.pokemon)
            assertNotNull("Failure state error should not be null", failureState.error)
            assertEquals("Failure state error message should match", errorMessage, failureState.error)

            // Ensure no more state changes
            expectNoEvents()
        }

        // Verify repository call
        coVerify(exactly = 1) { repository.fetchPokemonDetail(testPokemonId.toString()) }
    }

    @Test
    fun `retry fetch after failure success updates state`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange initial failure
        val initialErrorMessage = "Initial Network Error"
        coEvery {
            repository.fetchPokemonDetail(testPokemonId.toString())
        } returns Result.failure(RuntimeException(initialErrorMessage))

        // Initialize ViewModel to trigger the initial failure fetch
        viewModel = PokemonDetailViewModel(savedStateHandle, repository)

        viewModel.state.test {
            // Consume initial loading and failure states
            awaitItem() // Loading
            val failureState = awaitItem() // Failure
            assertEquals("Should be in failure state initially", initialErrorMessage, failureState.error)

            // Arrange for retry success
            val successResponse = createDummyDetailResponse(testPokemonId, "Retry Pikachu")
            coEvery {
                repository.fetchPokemonDetail(testPokemonId.toString())
            } returns Result.success(successResponse)

            // Act: Manually call fetchDetails again
            viewModel.fetchDetails()

            // Assert:
            // 1. Loading state during retry
            val retryLoadingState = awaitItem()
            assertTrue("Should be loading during retry", retryLoadingState.isLoading)
            assertNull("Error should be cleared during retry loading", retryLoadingState.error)
            // Pokemon data should still be null from the previous failure state while loading
            assertNull("Pokemon data should be null during retry loading", retryLoadingState.pokemon)

            // 2. Success state after retry
            val retrySuccessState = awaitItem()
            assertFalse("Should not be loading after retry success", retrySuccessState.isLoading)
            assertNull("Error should be null after retry success", retrySuccessState.error)
            assertNotNull("Pokemon should be present after retry success", retrySuccessState.pokemon)
            assertEquals("Pokemon name should match retry response", "Retry Pikachu", retrySuccessState.pokemon?.name)

            expectNoEvents()
        }

        // Verify repository was called twice (initial failure within test + retry)
        coVerify(exactly = 2) { repository.fetchPokemonDetail(testPokemonId.toString()) }
    }

} 