package io.bitrise.sample.android.mockserver.ui.catfacts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.bitrise.sample.android.mockserver.Dependencies
import io.bitrise.sample.android.mockserver.R

@Composable
fun CatFact(vm: CatViewModel = viewModel<CatViewModel>(factory = Dependencies.ViewModelFactory)) {
    val state by vm.state.collectAsState()

    val onNewCatFact: () -> Unit = { vm.fetchCatFact() }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onNewCatFact,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(text = "Get a new cat fact")
        }
        when (state) {
            is CatFactState.Initial -> {
                // Empty state
            }
            is CatFactState.Loading -> {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            is CatFactState.Success -> {
                Text(
                    text = (state as CatFactState.Success).fact,
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }

            is CatFactState.Failure -> {
                Text(text = stringResource(R.string.cat_fact_load_error))
            }
        }
        Image(
            painter = painterResource(R.drawable.logo_bitrise),
            contentDescription = "Bitrise",
            modifier = Modifier.fillMaxWidth()
        )
    }
}