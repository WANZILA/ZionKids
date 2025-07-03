package com.example.zionkids.presentation.screens.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zionkids.presentation.viewModels.KidRegistrationViewModel

@Composable
fun KidRegistrationForm(viewModel: KidRegistrationViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Hello"
        )
    }
}