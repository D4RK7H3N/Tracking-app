package com.sagay.touristnav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sagay.touristnav.ui.LaunchScreen
import com.sagay.touristnav.ui.MapScreen
import com.sagay.touristnav.ui.Motion
import com.sagay.touristnav.ui.OnboardingPrefs
import com.sagay.touristnav.ui.OnboardingScreen
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
                    val context = remember { this@MainActivity }
                    var onboardingDone by rememberSaveable {
                        mutableStateOf(OnboardingPrefs.isSeen(context))
                    }
                    var launchDone by rememberSaveable { mutableStateOf(false) }

                    // 0 = first-run tour, 1 = branded launch, 2 = map.
                    val stage = when {
                        !onboardingDone -> 0
                        !launchDone -> 1
                        else -> 2
                    }
                    val pageDuration = Motion.duration(Motion.PAGE_MS)
                    AnimatedContent(
                        targetState = stage,
                        transitionSpec = {
                            val duration = pageDuration
                            (
                                fadeIn(animationSpec = tween(duration)) +
                                    slideInHorizontally(
                                        animationSpec = tween(duration),
                                        initialOffsetX = { it / 12 },
                                    )
                                ) togetherWith (
                                fadeOut(animationSpec = tween(duration)) +
                                    slideOutHorizontally(
                                        animationSpec = tween(duration),
                                        targetOffsetX = { -it / 12 },
                                    )
                                )
                        },
                        label = "entry-flow",
                    ) { current ->
                        when (current) {
                            0 -> OnboardingScreen(
                                onFinish = {
                                    OnboardingPrefs.markSeen(context)
                                    onboardingDone = true
                                },
                            )
                            1 -> LaunchScreen(onDone = { launchDone = true })
                            else -> MapScreen()
                        }
                    }
                }
            }
        }
    }
}
