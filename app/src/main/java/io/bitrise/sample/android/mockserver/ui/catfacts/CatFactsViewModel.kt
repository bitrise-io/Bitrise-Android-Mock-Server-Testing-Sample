package io.bitrise.sample.android.mockserver.ui.catfacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.bitrise.sample.android.mockserver.data.CatFactService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

sealed class CatFactState {
    data object Initial : CatFactState()
    data object Loading : CatFactState()
    data class Success(val fact: String) : CatFactState()
    data object Failure : CatFactState()
}

class CatViewModel(
    private val catFactService: CatFactService
) : ViewModel() {

    val state = MutableStateFlow<CatFactState>(CatFactState.Initial)

    fun fetchCatFact() {
        viewModelScope.launch {
            try {
                state.value = CatFactState.Loading
                val response = catFactService.getCatFact()
                state.value = CatFactState.Success(response.fact)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { e.asLog() }
                state.value = CatFactState.Failure
            }
        }
    }
}