package com.example.zionkids.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zionkids.presentation.viewModels.KidRegistrationViewModel

@Composable
fun KidRegistrationScreen(

    viewModel: KidRegistrationViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    val kids by viewModel.kids.collectAsState()

    // Example output
//    Column {
//        kids.forEach {
//            Text(text = "${it.name}, Age ${it.age}")
//        }
//    }
}
