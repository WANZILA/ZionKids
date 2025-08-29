package com.example.zionkids.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zionkids.presentation.screens.widgets.ErrorState
import com.example.zionkids.presentation.screens.widgets.ChildItem
import com.example.zionkids.presentation.screens.widgets.KidRegistrationForm
import com.example.zionkids.presentation.screens.widgets.LoadingState
import com.example.zionkids.presentation.viewModels.KidRegisUiState
import com.example.zionkids.presentation.viewModels.KidRegistrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidRegistrationScreen(

    viewModel: KidRegistrationViewModel = hiltViewModel()
) {
//    val kids by viewModel.kids.collectAsState(initial = emptyList())
//    val uiState by viewModel.uiState.collectAsState()
//
//    var fetchData by rememberSaveable { mutableStateOf(0)}
//
//    if(fetchData == 0){
//        viewModel.fetchData()
//        fetchData = fetchData.inc()
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(title = { Text("Kids Registration")})
//        },
//        content = {
//            Box(modifier = Modifier.padding(it))
//            {
//                when (val state = uiState){
//                    is KidRegisUiState.Error -> {
//                        ErrorState(
//                            errorMessage = state.errorMessage,
//                            onRetry = {}
//                        )
//                    }
//
//                    KidRegisUiState.Loaded -> LoadingState("Loading data ..")
//
//                    KidRegisUiState.Loading -> {
//                        LazyColumn(
//                            modifier = Modifier.fillMaxSize(),
//                            verticalArrangement = Arrangement.spacedBy(10.dp),
//                            contentPadding =  PaddingValues(horizontal = 10.dp)
//                        ) {
//                            items(kids) { kid ->
//                                ChildItem(
//                                    kid = kid,
//                                    onClicked = {},
//                                    )
//                            }
//                        }
//                    }
//
//                    KidRegisUiState.Editing -> KidRegistrationForm(viewModel)
//                }
//            }
//        }
//    )

//    LaunchedEffect(Unit) {
//        viewModel.fetchData()
//    }
//
//    val kids by viewModel.kids.collectAsState()

    // Example output
//    Column {
//        kids.forEach {
//            Text(text = "${it.name}, Age ${it.age}")
//        }
//    }
}
/**
 *
 * @Composable
 * fun KidRegistrationScreen(viewModel: KidRegistrationViewModel = hiltViewModel()) {
 *     // Trigger data fetch once
 *     LaunchedEffect(Unit) {
 *         viewModel.fetchData()
 *     }
 *
 *     val kids by viewModel.kids.collectAsState()
 *     val uiState by viewModel.uiState.collectAsState()
 *
 *     Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
 *         when (uiState) {
 *             is KidRegisUiState.Loading -> {
 *                 CircularProgressIndicator()
 *             }
 *
 *             is KidRegisUiState.Error -> {
 *                 Text("Error: ${(uiState as KidRegisUiState.Error).errorMessage}")
 *             }
 *
 *             else -> {
 *                 Text("Registered Kids", style = MaterialTheme.typography.h6)
 *                 Spacer(modifier = Modifier.height(16.dp))
 *
 *                 LazyColumn {
 *                     items(kids) { kid ->
 *                         Text("- ${kid.name}, age ${kid.age}")
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * */