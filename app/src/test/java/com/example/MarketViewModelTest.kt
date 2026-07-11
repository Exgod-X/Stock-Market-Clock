package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.MarketStatus
import com.example.viewmodel.MarketViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarketViewModelTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun testToggleSoundNotification() = runTest {
        val viewModel = MarketViewModel(application)
        val marketId = "1" // NYSE or first market

        // Initially sound should not be enabled
        var uiState = viewModel.uiState.value
        assertFalse(uiState.soundEnabledMarketIds.contains(marketId))

        // Toggle it on
        viewModel.toggleSoundNotification(marketId)
        uiState = viewModel.uiState.value
        assertTrue(uiState.soundEnabledMarketIds.contains(marketId))

        // Toggle it off
        viewModel.toggleSoundNotification(marketId)
        uiState = viewModel.uiState.value
        assertFalse(uiState.soundEnabledMarketIds.contains(marketId))
    }

    @Test
    fun testSharedPreferencesPersistence() = runTest {
        // Toggle sound on in one ViewModel instance
        var viewModel = MarketViewModel(application)
        val marketId = "2" // NASDAQ or second market
        viewModel.toggleSoundNotification(marketId)

        // Verify in a new ViewModel instance that it persists
        val newViewModel = MarketViewModel(application)
        val uiState = newViewModel.uiState.value
        assertTrue(uiState.soundEnabledMarketIds.contains(marketId))
    }
}
