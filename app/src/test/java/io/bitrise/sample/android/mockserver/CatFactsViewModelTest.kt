package io.bitrise.sample.android.mockserver

import app.cash.turbine.test
import io.bitrise.sample.android.mockserver.data.CatFactService
import io.bitrise.sample.android.mockserver.ui.catfacts.CatFactState
import io.bitrise.sample.android.mockserver.ui.catfacts.CatViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class CatFactsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockedCatFactService = Retrofit.Builder()
        .baseUrl("http://localhost:3001")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(CatFactService::class.java)

    @Test
    fun `Given no interaction When ViewModel is created Then cat fact is empty`() = runTest {
        val viewModel = CatViewModel(mockedCatFactService)

        viewModel.state.test {
            assertEquals(CatFactState.Initial, awaitItem())
        }
    }

    @Test
    fun `Given default state When a fact is requested Then the fact becomes available`() = runTest {
        val viewModel = CatViewModel(mockedCatFactService)

        viewModel.state.test {
            assertEquals(CatFactState.Initial, awaitItem())

            viewModel.fetchCatFact()

            assertEquals(CatFactState.Loading, awaitItem())

            val expectedFact = "In 1997, NASA's Mars Pathfinder mission delivered a robotic rover named Sojourner to the surface of Mars. This marked the first time a mobile robot roamed another planet, paving the way for future exploration rovers like Spirit, Opportunity, Curiosity, and Perseverance."
            assertEquals(CatFactState.Success(expectedFact), awaitItem())
        }
    }

}