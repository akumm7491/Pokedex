package com.akumm7491.pokedex.ui.viewmodels

import app.cash.turbine.test
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.akumm7491.pokedex.domain.models.PokemonListItem
import com.akumm7491.pokedex.domain.models.PokemonListResponse
import com.akumm7491.pokedex.domain.repositories.PokemonRepository
import com.akumm7491.pokedex.utils.Constants
import com.akumm7491.pokedex.utils.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi // Needed for TestCoroutineDispatcher
class PokemonListViewModelTest {

    // Rule to swap the Main dispatcher with a TestDispatcher
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    // Mocks
    private lateinit var repository: PokemonRepository

    // Class Under Test
    private lateinit var viewModel: PokemonListViewModel

    @Before
    fun setUp() {
        // Create a fresh mock before each test
        repository = mockk()
        // Instantiate ViewModel AFTER setting the main dispatcher via the rule
        // and creating the mock
         viewModel = PokemonListViewModel(repository)
    }

    // Helper function to create dummy PokemonListResponse
    private fun createDummyResponse(count: Int, next: String?, previous: String?, results: List<PokemonListItem>): PokemonListResponse {
        return PokemonListResponse(count = count, next = next, previous = previous, results = results)
    }

    // Helper function to create dummy PokemonListItem
    private fun createDummyItem(id: Int, name: String): PokemonListItem {
        return PokemonListItem(name = name, url = "${Constants.BASE_URL}pokemon/$id/")
    }

    // --- Test Cases --- 

    @Test
    fun `initial load success updates state correctly`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange
        val initialUrl = Constants.BASE_URL
        val nextPageUrl = "${Constants.BASE_URL}pokemon?offset=20&limit=20"
        val initialItems = listOf(createDummyItem(1, "Bulbasaur"), createDummyItem(2, "Ivysaur"))
        val response = createDummyResponse(151, nextPageUrl, null, initialItems)
        
        // Mock the repository call for the initial load
        coEvery { repository.fetchPokemon(initialUrl) } returns Result.success(response)

        // Act & Assert using Turbine
        viewModel.state.test {
            // 1. Initial default state (emitted before init{} block calls loadData)
            val defaultState = awaitItem()
            assertTrue("Default state should not be loading", !defaultState.isLoadingInitial && !defaultState.isLoadingMore)
            assertTrue("Default state should have empty items", defaultState.items.isEmpty())
            assertNull("Default state should have no error", defaultState.error)
            assertEquals("Default state nextUrl should be base", Constants.BASE_URL, defaultState.nextUrl)
            assertTrue("Default state should be able to load more", defaultState.canLoadMore)

            // 2. State during initial load (triggered by init block)
            val loadingState = awaitItem()
            assertTrue("Should be loading initial", loadingState.isLoadingInitial)
            assertFalse("Should not be loading more", loadingState.isLoadingMore)
            assertNull("Error should be null during loading", loadingState.error)
            // Items, nextUrl, canLoadMore should still be the default ones while loading
            assertEquals("Items should be default during loading", defaultState.items, loadingState.items)
            assertEquals("nextUrl should be default during loading", defaultState.nextUrl, loadingState.nextUrl)
            assertEquals("canLoadMore should be default during loading", defaultState.canLoadMore, loadingState.canLoadMore)

            // 3. State after successful initial load
            val successState = awaitItem()
            assertFalse("Should not be loading initial after success", successState.isLoadingInitial)
            assertFalse("Should not be loading more after success", successState.isLoadingMore)
            assertNull("Error should be null after success", successState.error)
            assertEquals("Items should be updated after success", initialItems, successState.items)
            assertEquals("nextUrl should be updated after success", nextPageUrl, successState.nextUrl)
            assertTrue("Should be able to load more after success", successState.canLoadMore)
            
            // Ensure no more state changes unexpectedly
            expectNoEvents()
        }
        
        // Verify the repository was called exactly once with the correct URL
        coVerify(exactly = 1) { repository.fetchPokemon(initialUrl) }
    }

    @Test
    fun `initial load failure updates state correctly`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange
        val initialUrl = Constants.BASE_URL
        val errorMessage = "Network Error"
        val exception = RuntimeException(errorMessage)

        // Mock the repository call for the initial load to fail
        coEvery { repository.fetchPokemon(initialUrl) } returns Result.failure(exception)

        // Act & Assert using Turbine
        viewModel.state.test {
            // 1. Initial default state
            val defaultState = awaitItem()
            assertTrue("Default state should not be loading", !defaultState.isLoadingInitial && !defaultState.isLoadingMore)
            assertTrue("Default state should have empty items", defaultState.items.isEmpty())
            assertNull("Default state should have no error", defaultState.error)

            // 2. State during initial load
            val loadingState = awaitItem()
            assertTrue("Should be loading initial", loadingState.isLoadingInitial)
            assertNull("Error should be null during loading", loadingState.error)

            // 3. State after failed initial load
            val failureState = awaitItem()
            assertFalse("Should not be loading initial after failure", failureState.isLoadingInitial)
            assertFalse("Should not be loading more after failure", failureState.isLoadingMore)
            assertNotNull("Error should be present after failure", failureState.error)
            assertEquals("Error message should match", errorMessage, failureState.error)
            // Items, nextUrl, canLoadMore should remain the default ones after failure
            assertEquals("Items should be default after failure", defaultState.items, failureState.items)
            assertEquals("nextUrl should be default after failure", defaultState.nextUrl, failureState.nextUrl)
            assertEquals("canLoadMore should be default after failure", defaultState.canLoadMore, failureState.canLoadMore)

            // Ensure no more state changes unexpectedly
            expectNoEvents()
        }

        // Verify the repository was called exactly once
        coVerify(exactly = 1) { repository.fetchPokemon(initialUrl) }
    }

    @Test
    fun `load more success appends items and updates state`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange: First, simulate a successful initial load to get a valid 'nextUrl'
        val initialUrl = Constants.BASE_URL
        val page1NextUrl = "${Constants.BASE_URL}pokemon?offset=2&limit=2"
        val page2NextUrl = "${Constants.BASE_URL}pokemon?offset=4&limit=2"
        val initialItems = listOf(createDummyItem(1, "Bulbasaur"), createDummyItem(2, "Ivysaur"))
        val page1Response = createDummyResponse(151, page1NextUrl, null, initialItems)

        // Mock initial load
        coEvery { repository.fetchPokemon(initialUrl) } returns Result.success(page1Response)

        // Need to consume initial states to allow the load to finish before triggering 'load more'
        viewModel.state.test {
            awaitItem() // Default state
            awaitItem() // Loading state
            awaitItem() // Success state (after initial load)

            // Arrange for Load More
            val moreItems = listOf(createDummyItem(3, "Venusaur"), createDummyItem(4, "Charmander"))
            val page2Response = createDummyResponse(151, page2NextUrl, initialUrl, moreItems)
            
            // Mock the repository call for the 'load more' using the nextUrl from page 1
            coEvery { repository.fetchPokemon(page1NextUrl) } returns Result.success(page2Response)

            // Act: Trigger LoadMoreItems intent
            viewModel.processIntent(PokemonListIntent.LoadMoreItems)

            // Assert Load More states
            // 1. State during load more
            val loadingMoreState = awaitItem()
            assertFalse("Should not be loading initial during load more", loadingMoreState.isLoadingInitial)
            assertTrue("Should be loading more", loadingMoreState.isLoadingMore)
            assertNull("Error should be null during load more", loadingMoreState.error)
            assertEquals("Items should be initial items during load more", initialItems, loadingMoreState.items)
            assertEquals("nextUrl should be page 1 nextUrl during load more", page1NextUrl, loadingMoreState.nextUrl)

            // 2. State after successful load more
            val successMoreState = awaitItem()
            assertFalse("Should not be loading initial after load more success", successMoreState.isLoadingInitial)
            assertFalse("Should not be loading more after load more success", successMoreState.isLoadingMore)
            assertNull("Error should be null after load more success", successMoreState.error)
            assertEquals("Items should be appended after load more", initialItems + moreItems, successMoreState.items)
            assertEquals("nextUrl should be updated after load more", page2NextUrl, successMoreState.nextUrl)
            assertTrue("Should still be able to load more", successMoreState.canLoadMore)

            // Ensure no more state changes unexpectedly
            expectNoEvents()
        }

        // Verify the repository was called for initial load AND load more
        coVerify(exactly = 1) { repository.fetchPokemon(initialUrl) }
        coVerify(exactly = 1) { repository.fetchPokemon(page1NextUrl) }
    }

    @Test
    fun `load more failure updates state with error`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange: First, simulate a successful initial load
        val initialUrl = Constants.BASE_URL
        val page1NextUrl = "${Constants.BASE_URL}pokemon?offset=2&limit=2"
        val initialItems = listOf(createDummyItem(1, "Bulbasaur"), createDummyItem(2, "Ivysaur"))
        val page1Response = createDummyResponse(151, page1NextUrl, null, initialItems)

        coEvery { repository.fetchPokemon(initialUrl) } returns Result.success(page1Response)

        viewModel.state.test {
            awaitItem() // Default
            awaitItem() // Loading Initial
            val initialSuccessState = awaitItem() // Success Initial

            // Arrange for Load More Failure
            val errorMessage = "Failed to fetch more"
            val exception = RuntimeException(errorMessage)
            coEvery { repository.fetchPokemon(page1NextUrl) } returns Result.failure(exception)

            // Act
            viewModel.processIntent(PokemonListIntent.LoadMoreItems)

            // Assert
            // 1. Loading More State
            val loadingMoreState = awaitItem()
            assertTrue("Should be loading more", loadingMoreState.isLoadingMore)
            assertNull("Error should be null during loading more", loadingMoreState.error)

            // 2. Failure State
            val failureState = awaitItem()
            assertFalse("Should not be loading more after failure", failureState.isLoadingMore)
            assertNotNull("Error should be present after load more failure", failureState.error)
            assertEquals("Error message should match", errorMessage, failureState.error)
            // Items and pagination should remain as they were after the initial success
            assertEquals("Items should be initial items after failure", initialItems, failureState.items)
            assertEquals("nextUrl should be page 1 nextUrl after failure", page1NextUrl, failureState.nextUrl)
            assertTrue("Should still be able to load more (retry possible)", failureState.canLoadMore)

            expectNoEvents()
        }

        // Verify calls
        coVerify(exactly = 1) { repository.fetchPokemon(initialUrl) }
        coVerify(exactly = 1) { repository.fetchPokemon(page1NextUrl) }
    }

    @Test
    fun `load more success with null nextUrl updates canLoadMore to false`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange: Initial load
        val initialUrl = Constants.BASE_URL
        val page1NextUrl = "${Constants.BASE_URL}pokemon?offset=149&limit=2" // Simulate near end
        val initialItems = List(149) { createDummyItem(it + 1, "Pokemon${it + 1}") } // Items 1-149
        val page1Response = createDummyResponse(151, page1NextUrl, null, initialItems)

        coEvery { repository.fetchPokemon(initialUrl) } returns Result.success(page1Response)

        viewModel.state.test {
            awaitItem() // Default
            awaitItem() // Loading Initial
            awaitItem() // Success Initial

            // Arrange: Load More (Last Page)
            val lastItems = listOf(createDummyItem(150, "Mewtwo"), createDummyItem(151, "Mew"))
            // Response indicates this is the last page (next = null)
            val lastPageResponse = createDummyResponse(151, null, initialUrl, lastItems)
            coEvery { repository.fetchPokemon(page1NextUrl) } returns Result.success(lastPageResponse)

            // Act
            viewModel.processIntent(PokemonListIntent.LoadMoreItems)

            // Assert
            awaitItem() // Loading More state
            val lastPageState = awaitItem() // Final state

            assertFalse("Should not be loading after last page", lastPageState.isLoadingMore)
            assertNull("Error should be null", lastPageState.error)
            assertEquals("Items should include last items", initialItems + lastItems, lastPageState.items)
            assertNull("nextUrl should be null after last page", lastPageState.nextUrl)
            assertFalse("canLoadMore should be false after last page", lastPageState.canLoadMore)

            // Act: Try loading more again (should not trigger fetch)
            viewModel.processIntent(PokemonListIntent.LoadMoreItems)

            // Assert: No state change and no further network calls
            expectNoEvents() // No new state should be emitted
        }

        // Verify repo calls: initial and the load more for the last page, but not the third attempt
        coVerify(exactly = 1) { repository.fetchPokemon(initialUrl) }
        coVerify(exactly = 1) { repository.fetchPokemon(page1NextUrl) }
    }

    @Test
    fun `update search query updates state immediately`() = runTest(mainCoroutineRule.testDispatcher) {
        val query = "Pikachu"

        // Mock the initial load so it finishes quickly
        coEvery { repository.fetchPokemon(any()) } returns Result.success(createDummyResponse(0, null, null, emptyList()))

        viewModel.state.test {
            // Consume initial states from the automatic init load
            awaitItem() // Default
            awaitItem() // Loading
            awaitItem() // Success/Failure

            // Act: Send the search query update
            viewModel.processIntent(PokemonListIntent.UpdateSearchQuery(query))

            // Assert: The next state emitted should have the updated query
            val updatedState = awaitItem()
            assertEquals("Search query should be updated in state", query, updatedState.searchQuery)

            // Cancel remaining events *after* assertions
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filteredPokemonList updates after debounce`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange: Simulate initial load with some items
        val initialUrl = Constants.BASE_URL
        val items = listOf(
            createDummyItem(1, "Bulbasaur"),
            createDummyItem(25, "Pikachu"),
            createDummyItem(4, "Charmander"),
            createDummyItem(26, "Raichu")
        )
        val response = createDummyResponse(4, null, null, items)
        coEvery { repository.fetchPokemon(initialUrl) } returns Result.success(response)

        turbineScope {
            // Start turbines for both flows concurrently
            val stateTurbine = viewModel.state.testIn(backgroundScope, name = "Main State Flow")
            val filteredListTurbine = viewModel.filteredPokemonList.testIn(backgroundScope, name = "Filtered List Flow")

            // --- Initial Load Consumption ---
            // 1. Initial Default State (from stateTurbine)
            val defaultState = stateTurbine.awaitItem()
            assertTrue(defaultState.items.isEmpty())

            // 2. Initial Empty Filtered List (from filteredListTurbine)
            assertEquals(emptyList<PokemonListItem>(), filteredListTurbine.awaitItem())

            // 3. Loading State (from stateTurbine)
            val loadingState = stateTurbine.awaitItem()
            assertTrue(loadingState.isLoadingInitial)

            // 4. Success State (from stateTurbine)
            val initialSuccessState = stateTurbine.awaitItem()
            assertFalse(initialSuccessState.isLoadingInitial)
            assertEquals(items, initialSuccessState.items)

            // 5. Filtered List after load (query blank) (from filteredListTurbine)
            assertEquals(items, filteredListTurbine.awaitItem())

            // --- Test Search Query 1 ("chu") ---
            val query1 = "chu"
            viewModel.processIntent(PokemonListIntent.UpdateSearchQuery(query1))
            // Assert main state updated immediately
            val state1 = stateTurbine.awaitItem()
            assertEquals(query1, state1.searchQuery)
            // Assert filtered list DID NOT update yet (debounce)
            filteredListTurbine.expectNoEvents()
            // Advance time past debounce
            advanceTimeBy(301)
            // Assert filtered list updated
            val filteredItems1 = listOf(items[1], items[3]) // Pikachu, Raichu
            assertEquals(filteredItems1, filteredListTurbine.awaitItem())

            // --- Test Search Query 2 ("Bulba") ---
            val query2 = "Bulba"
            viewModel.processIntent(PokemonListIntent.UpdateSearchQuery(query2))
            // Assert main state updated immediately
            val state2 = stateTurbine.awaitItem()
            assertEquals(query2, state2.searchQuery)
            // Assert filtered list DID NOT update yet (debounce)
            filteredListTurbine.expectNoEvents()
            // Advance time past debounce
            advanceTimeBy(301)
            // Assert filtered list updated
            val filteredItems2 = listOf(items[0]) // Bulbasaur
            assertEquals(filteredItems2, filteredListTurbine.awaitItem())

            // --- Test Search Query 3 (Empty "") ---
            viewModel.processIntent(PokemonListIntent.UpdateSearchQuery(""))
            // Assert main state updated immediately
            val state3 = stateTurbine.awaitItem()
            assertEquals("", state3.searchQuery)
            // Assert filtered list updated immediately (no debounce for empty)
            assertEquals(items, filteredListTurbine.awaitItem())

            // --- Cleanup ---
            stateTurbine.cancelAndConsumeRemainingEvents()
            filteredListTurbine.cancelAndConsumeRemainingEvents()
        }

        // Verify initial load happened once
        coVerify(exactly = 1) { repository.fetchPokemon(initialUrl) }
    }

    @Test
    fun `clear error intent removes error from state`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange: Simulate an initial load failure to get an error state
        val initialUrl = Constants.BASE_URL
        val errorMessage = "Network Error"
        coEvery { repository.fetchPokemon(initialUrl) } returns Result.failure(RuntimeException(errorMessage))

        viewModel.state.test {
            awaitItem() // Default
            awaitItem() // Loading
            val failureState = awaitItem() // Failure state
            assertEquals("Error should be present initially", errorMessage, failureState.error)

            // Act: Send ClearError intent
            viewModel.processIntent(PokemonListIntent.ClearError)

            // Assert: Error should be null in the next state
            val clearedState = awaitItem()
            assertNull("Error should be null after clearing", clearedState.error)
            // Other parts of the state should remain unchanged from the failure state
            assertEquals(failureState.items, clearedState.items)
            assertEquals(failureState.nextUrl, clearedState.nextUrl)
            assertEquals(failureState.canLoadMore, clearedState.canLoadMore)
            assertEquals(failureState.isLoadingInitial, clearedState.isLoadingInitial)
            assertEquals(failureState.isLoadingMore, clearedState.isLoadingMore)

            expectNoEvents()
        }
    }

    @Test
    fun `retry initial load intent re-triggers fetch`() = runTest(mainCoroutineRule.testDispatcher) {
        // Arrange: Simulate an initial load failure
        val initialUrl = Constants.BASE_URL
        val errorMessage = "Network Error"
        coEvery { repository.fetchPokemon(initialUrl) } returns Result.failure(RuntimeException(errorMessage))

        viewModel.state.test {
            awaitItem() // Default
            awaitItem() // Loading
            awaitItem() // Failure

            // Arrange for retry success
            val retryItems = listOf(createDummyItem(1, "Bulbasaur"))
            val successResponse = createDummyResponse(1, null, null, retryItems)
            // Make the repo succeed on the *next* call
            coEvery { repository.fetchPokemon(initialUrl) } returns Result.success(successResponse)

            // Act: Send RetryInitialLoad intent
            viewModel.processIntent(PokemonListIntent.RetryInitialLoad)

            // Assert:
            // 1. Loading state again
            val loadingState = awaitItem()
            assertTrue("Should be loading initial on retry", loadingState.isLoadingInitial)
            assertNull("Error should be cleared on retry start", loadingState.error)

            // 2. Success state after retry
            val successState = awaitItem()
            assertFalse("Should not be loading after retry success", successState.isLoadingInitial)
            assertNull("Error should be null after retry success", successState.error)
            assertEquals("Items should be updated after retry success", retryItems, successState.items)
            assertNull("nextUrl should be null from retry response", successState.nextUrl)
            assertFalse("canLoadMore should be false from retry response", successState.canLoadMore)

            expectNoEvents()
        }

        // Verify repository was called twice with the same initial URL
        coVerify(exactly = 2) { repository.fetchPokemon(initialUrl) }
    }
} 