package com.akumm7491.pokedex.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.hasProgressBarRangeInfo


class InfiniteScrollListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Helper data class for test items
    data class TestItem(val id: Int, val name: String)

    private val testItems = List(20) { TestItem(it, "Item $it") }

    @Test
    fun displaysInitialItemsCorrectly() {
        composeTestRule.setContent {
            InfiniteScrollList(
                items = testItems.take(5),
                itemKey = { it.id },
                itemContent = { item -> Text(item.name) },
                isLoadingMore = false,
                canLoadMore = true,
                onLoadMore = { /* Do nothing */ }
            )
        }

        // Verify first 5 items are displayed
        composeTestRule.onNodeWithText("Item 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item 4").assertIsDisplayed()

        // Verify an item not in the initial list is not displayed
        composeTestRule.onNodeWithText("Item 5").assertDoesNotExist()
    }

    @Test
    fun showsLoadingIndicator_when_isLoadingMore_and_canLoadMore_areTrue() {
        composeTestRule.setContent {
            InfiniteScrollList(
                items = testItems.take(3),
                itemKey = { it.id },
                itemContent = { item -> Text(item.name) },
                isLoadingMore = true,
                canLoadMore = true,
                onLoadMore = { /* Do nothing */ }
            )
        }

        // Verify items are displayed
        composeTestRule.onNodeWithText("Item 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item 2").assertIsDisplayed()

        // Verify the default loading indicator (CircularProgressIndicator) is shown
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun hidesLoadingIndicator_when_isLoadingMore_isFalse() {
        composeTestRule.setContent {
            InfiniteScrollList(
                items = testItems.take(3),
                itemKey = { it.id },
                itemContent = { item -> Text(item.name) },
                isLoadingMore = false,
                canLoadMore = true,
                onLoadMore = { /* Do nothing */ }
            )
        }

        // Verify items are displayed
        composeTestRule.onNodeWithText("Item 0").assertIsDisplayed()

        // Verify the loading indicator is NOT shown
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun hidesLoadingIndicator_when_canLoadMore_isFalse() {
        composeTestRule.setContent {
            InfiniteScrollList(
                items = testItems.take(3),
                itemKey = { it.id },
                itemContent = { item -> Text(item.name) },
                isLoadingMore = true,
                canLoadMore = false,
                onLoadMore = { /* Do nothing */ }
            )
        }

        // Verify items are displayed
        composeTestRule.onNodeWithText("Item 0").assertIsDisplayed()

        // Verify the loading indicator is NOT shown
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate), useUnmergedTree = true)
            .assertDoesNotExist()
    }


    @SuppressLint("RememberReturnType")
    @Test
    fun callsOnLoadMore_when_scrolledNearEnd_and_conditionsMet() {
        var loadMoreCalled = false
        val listState = mutableStateOf<LazyListState?>(null)
        val scrollThreshold = 5 // Trigger when 5 items from end are visible

        composeTestRule.setContent {
             val state = rememberLazyListState()
             // Capture the state for manipulation - use remember to avoid recomposition issues
             remember { listState.value = state }
             InfiniteScrollList(
                items = testItems,
                itemKey = { it.id },
                // Ensure items have some height for scrolling to work reliably
                itemContent = { item -> Text(item.name, modifier = Modifier.height(60.dp)) },
                isLoadingMore = false,
                canLoadMore = true,
                onLoadMore = { loadMoreCalled = true },
                listState = state,
                scrollThreshold = scrollThreshold
            )
        }

        // Ensure state is captured - not strictly necessary with remember but good practice
        composeTestRule.waitUntil(timeoutMillis = 5000) { listState.value != null }

        // Scroll towards the end
        // Target index: total items - scrollThreshold - 1 (index becomes visible)
        val targetIndex = testItems.size - scrollThreshold - 1
        composeTestRule.onNode(hasScrollAction())
            .performScrollToIndex(targetIndex)

        // Wait for potential recomposition and LaunchedEffect execution
        composeTestRule.waitForIdle()

        // Assert that onLoadMore was called
        assertTrue("onLoadMore should have been called after scrolling near the end", loadMoreCalled)
    }

    @SuppressLint("RememberReturnType")
    @Test
    fun doesNotCallOnLoadMore_when_canLoadMore_isFalse() {
        var loadMoreCalled = false
        val listState = mutableStateOf<LazyListState?>(null)
        val scrollThreshold = 5

        composeTestRule.setContent {
             val state = rememberLazyListState()
             remember { listState.value = state }
             InfiniteScrollList(
                items = testItems,
                itemKey = { it.id },
                itemContent = { item -> Text(item.name, modifier = Modifier.height(60.dp)) },
                isLoadingMore = false,
                canLoadMore = false,
                onLoadMore = { loadMoreCalled = true },
                listState = state,
                 scrollThreshold = scrollThreshold
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) { listState.value != null }
        val targetIndex = testItems.size - scrollThreshold - 1

        composeTestRule.onNode(hasScrollAction())
            .performScrollToIndex(targetIndex)

        composeTestRule.waitForIdle()

        assertFalse("onLoadMore should NOT have been called when canLoadMore is false", loadMoreCalled)
    }

    @SuppressLint("RememberReturnType")
    @Test
    fun doesNotCallOnLoadMore_when_isLoadingMore_isTrue() {
        var loadMoreCalled = false
        val listState = mutableStateOf<LazyListState?>(null)
        val scrollThreshold = 5

        composeTestRule.setContent {
             val state = rememberLazyListState()
             remember { listState.value = state }
             InfiniteScrollList(
                items = testItems,
                itemKey = { it.id },
                itemContent = { item -> Text(item.name, modifier = Modifier.height(60.dp)) },
                isLoadingMore = true,
                canLoadMore = true,
                onLoadMore = { loadMoreCalled = true },
                listState = state,
                 scrollThreshold = scrollThreshold
            )
        }

       composeTestRule.waitUntil(timeoutMillis = 5000) { listState.value != null }
       val targetIndex = testItems.size - scrollThreshold - 1

        composeTestRule.onNode(hasScrollAction())
            .performScrollToIndex(targetIndex)

        composeTestRule.waitForIdle()

        assertFalse("onLoadMore should NOT have been called when isLoadingMore is true", loadMoreCalled)
    }
} 