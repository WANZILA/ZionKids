package com.example.zionkids

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import com.example.zionkids.presentation.navigation.ZionAppNavHost
import com.example.zionkids.presentation.screens.AllComponentsScreen
import com.example.zionkids.presentation.screens.ChildBasicInfoScreen
import com.example.zionkids.presentation.screens.HomeScreen

import com.example.zionkids.presentation.theme.ZionKidsTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@Keep
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZionKidsTheme(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = false
            )  {
                SetBarsToPrimary()
                ZionAppNavHost()
            }
//            ZionKidsTheme(           // <- must wrap EVERYTHING
//                darkTheme = isSystemInDarkTheme(),
//                dynamicColor = false   // keep false if you want your custom palette
//            )  {
//
//                //SetBarColor(color =  MaterialTheme.colorScheme.background)
//                /** increase the cache to 500mb of documents storage */
//                val cacheSettings = PersistentCacheSettings.newBuilder()
//                    .setSizeBytes(500L * 1024 * 1024) // 🔥 500MB
//
//                val settings = FirebaseFirestoreSettings.Builder()
//                    .setLocalCacheSettings(cacheSettings.build())
//                    .build()
//
//                Firebase.firestore.firestoreSettings = settings
//                SetBarsToPrimary()
//                ZionAppNavHost()
//              //  ChildBasicInfoScreen()
//            }
        }
    }
}

@Composable
private fun SetBarColor(color: androidx.compose.ui.graphics.Color){
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = color
        )
    }
}

@Composable
private fun SetBarsToPrimary() {
    val systemUiController = rememberSystemUiController()
    val primary = MaterialTheme.colorScheme.onSurfaceVariant
    val useDarkIcons = MaterialTheme.colorScheme.onSecondary.luminance() > 0.5f

    SideEffect {
        systemUiController.setStatusBarColor(
            color = primary,
            darkIcons = useDarkIcons
        )
        systemUiController.setNavigationBarColor(
            color = primary,
            darkIcons = useDarkIcons
        )
    }
}


