package com.akumm7491.pokedex.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit rule to set the main dispatcher to a TestCoroutineDispatcher for unit tests.
 * This allows controlling the execution of coroutines launched on the main dispatcher.
 */
@ExperimentalCoroutinesApi
class MainCoroutineRule(
    // Allows injecting a specific dispatcher if needed, otherwise creates a standard one.
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        // Set the main dispatcher to the test dispatcher before the test runs.
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        // Reset the main dispatcher to the original one after the test finishes.
        Dispatchers.resetMain()
    }
} 