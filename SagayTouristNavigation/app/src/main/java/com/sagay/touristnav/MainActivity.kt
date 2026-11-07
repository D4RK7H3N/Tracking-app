package com.sagay.touristnav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sagay.touristnav.ui.LaunchScreen
import com.sagay.touristnav.ui.MapScreen
import com.sagay.touristnav.ui.SagayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SagayTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Branded launch screen first (tap to skip); the
                    // permission flow starts only after it ends.
                    var launchDone by rememberSaveable { mutableStateOf(false) }
                    if (launchDone) {
                        MapScreen()
                    } else {
                        LaunchScreen(onDone = { launchDone = true })
                    }
                }
            }
        }
    }
}
