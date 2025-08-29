package com.example.zionkids.presentation.screens.splash

import com.example.zionkids.presentation.theme.ZionKidsTheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zionkids.R
import com.example.zionkids.presentation.viewModels.SplashViewModel

import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    toLogin: () -> Unit,
    toAdmin: () -> Unit,
    vm: SplashViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(2_000)
        if (vm.isLoggedIn()) toAdmin() else toLogin()
    }

    ZionKidsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center
        ) {
            // Centered logo + spinner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zion_kids_logo),
                    contentDescription = "UpLift Logo",
                    modifier = Modifier.size(150.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onTertiary,
                    strokeWidth = 4.dp
                )
            }

            // Tagline at bottom
            Text(
                text = "Children for Christ Jesus ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }
    }
}
