package com.akumm7491.pokedex.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * A reusable Jetpack Compose component for displaying a list with infinite scrolling behavior.
 *
 * @param T The type of items in the list.
 * @param items The current list of items of type [T] to display.
 * @param itemKey A function to extract a unique and stable key from an item. Used by LazyColumn.
 * @param itemContent A composable lambda responsible for rendering a single item of type [T].
 * @param isLoadingMore True if more items are currently being loaded, false otherwise. Used to show the loading indicator.
 * @param canLoadMore True if there are potentially more items to load, false if the end of the data has been reached.
 * @param onLoadMore A callback function invoked when the list is scrolled near the end and more data should be loaded.
 * @param modifier Optional Modifier for the LazyColumn container.
 * @param listState Optional LazyListState to control or observe the list's scroll state.
 * @param loadingIndicator Composable lambda for the loading indicator shown at the end while loading more.
 * @param scrollThreshold Number of items from the end to trigger onLoadMore. Defaults to 5.
 */
@Composable
fun <T> InfiniteScrollList(
    items: List<T>,
    itemKey: (item: T) -> Any,
    itemContent: @Composable (item: T) -> Unit,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    loadingIndicator: @Composable () -> Unit = { DefaultLoadingIndicator() },
    scrollThreshold: Int = 5
) {
    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(
            count = items.size,
            key = { index -> itemKey(items[index]) } // Use the provided key extractor
        ) { index ->
            itemContent(items[index]) // Use the provided item renderer
        }

        // Show the loading indicator at the end if necessary
        if (isLoadingMore && canLoadMore) {
            item {
                loadingIndicator()
            }
        }
    }

    // Effect to trigger 'onLoadMore' when scrolling near the end
    LaunchedEffect(listState, items.size, isLoadingMore, canLoadMore) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                // Calculate if scroll is near the end
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                // Ensure threshold isn't negative if list is small
                val effectiveThreshold = maxOf(0, scrollThreshold -1)
                lastVisibleItemIndex >= 0 && lastVisibleItemIndex >= items.size - 1 - effectiveThreshold
            }
            .distinctUntilChanged() // Only react to changes in the "near end" state
            .filter { isNearEnd -> isNearEnd } // Only proceed if near the end
            .collect {
                // Only trigger load more if not already loading and more items are available
                if (!isLoadingMore && canLoadMore && items.isNotEmpty()) {
                    onLoadMore()
                }
            }
    }
}

/**
 * Default loading indicator used by InfiniteScrollList.
 */
@Composable
fun DefaultLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}