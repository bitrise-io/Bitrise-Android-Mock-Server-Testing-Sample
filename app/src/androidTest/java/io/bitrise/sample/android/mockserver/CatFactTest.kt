package io.bitrise.sample.android.mockserver

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.bitrise.sample.android.mockserver.data.CatFactService
import io.bitrise.sample.android.mockserver.ui.catfacts.CatFact
import io.bitrise.sample.android.mockserver.ui.catfacts.CatViewModel
import io.bitrise.sample.android.mockserver.ui.theme.CatAppTheme
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class CatFactTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // We recreate the ViewModel for testing, but it connects to a different API endpoint
    private val viewModel = CatViewModel(
        catFactService = Retrofit.Builder()
            // // Since this code is running on the emulator, we use the special address 10.0.2.2 to reach the true localhost
            .baseUrl("http://10.0.2.2:3001")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(CatFactService::class.java)
    )

    @Test
    fun testInitialScreenState() {
        composeTestRule.setContent {
            CatAppTheme {
                CatFact(vm = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Get a new cat fact").assertExists()
    }

    @Test
    fun testGetNewCatFact() {
        composeTestRule.setContent {
            CatAppTheme {
                CatFact(vm = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Get a new cat fact").performClick()

        val expectedFact = "In 1997, NASA's Mars Pathfinder mission delivered a robotic rover named Sojourner to the surface of Mars. This marked the first time a mobile robot roamed another planet, paving the way for future exploration rovers like Spirit, Opportunity, Curiosity, and Perseverance."
        composeTestRule.onNodeWithText(expectedFact).assertExists()
    }
}
