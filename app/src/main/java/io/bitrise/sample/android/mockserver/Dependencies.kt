package io.bitrise.sample.android.mockserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import io.bitrise.sample.android.mockserver.ui.catfacts.CatViewModel

object Dependencies {
    val ViewModelFactory = object: ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val application = checkNotNull(extras[APPLICATION_KEY])

            return CatViewModel(
                (application as CatApp).catFactService,
            ) as T
        }
    }
}

