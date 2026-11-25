package com.sagay.touristnav.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.R
import com.sagay.touristnav.domain.models.VehicleType
import kotlinx.coroutines.launch

private enum class TourPage(val title: String, val body: String) {
    SEARCH(
        "Find your destination",
        "Search the destination list by name or category, then tap a place " +
            "to see the route, fare and the vehicle you need.",
    ),
    COLOUR(
        "Ask for the colour",
        "At the terminal, ask for the colour-coded vehicle: red and yellow " +
            "tricycles, pink buses and white boats. Each route has its own colour.",
    ),
    FARE(
        "Know the fare",
        "The fare is an official matrix estimate. Eligible riders get a 20% " +
            "discount — check the estimate before you board.",
    ),
}

/**
 * First-run tour: three swipeable pages with tappable dots, Skip and
 * Get started. All transitions go through Motion, so an OS/in-app
 * reduce-motion preference shortens them to zero.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = TourPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val pageDuration = Motion.duration(Motion.PAGE_MS)
    val microDuration = Motion.duration(Motion.MICRO_MS)
    val isLast = pagerState.currentPage == pages.lastIndex

    Surface(modifier = Modifier.fillMaxSize(), color = SagayColors.WarmMist) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onFinish) { Text("Skip") }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                OnboardingPage(page = page)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    val dotSize by animateFloatAsState(
                        targetValue = if (selected) 11f else 7f,
                        animationSpec = tween(microDuration, easing = Motion.easing),
                        label = "onboarding-dot",
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (selected) {
                            SagayColors.Coral
                        } else {
                            SagayColors.SoftHairline
                        },
                        animationSpec = tween(microDuration, easing = Motion.easing),
                        label = "onboarding-dot-colour",
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        index,
                                        animationSpec = tween(
                                            durationMillis = pageDuration,
                                            easing = Motion.easing,
                                        ),
                                    )
                                }
                            },
                    )
                    if (index != pages.lastIndex) Spacer(Modifier.size(8.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage - 1,
                                    animationSpec = tween(pageDuration, easing = Motion.easing),
                                )
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Previous page",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Back")
                    }
                }
                Spacer(Modifier.weight(1f))
                AnimatedContent(
                    targetState = isLast,
                    transitionSpec = {
                        fadeIn(tween(pageDuration)) togetherWith fadeOut(tween(pageDuration))
                    },
                    label = "onboarding-cta",
                ) { last ->
                    Button(
                        onClick = {
                            if (last) {
                                onFinish()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage + 1,
                                        animationSpec = tween(pageDuration, easing = Motion.easing),
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SagayColors.DeepOcean,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(if (last) "Get started" else "Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: Int) {
    val content = TourPage.entries[page]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SagayColors.sunriseGradient),
            contentAlignment = Alignment.Center,
        ) {
            when (page) {
                0 -> Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = SagayColors.DeepOceanDark,
                    modifier = Modifier.size(64.dp),
                )
                1 -> Icon(
                    painter = painterResource(R.drawable.ic_route),
                    contentDescription = null,
                    tint = SagayColors.DeepOceanDark,
                    modifier = Modifier.size(64.dp),
                )
                else -> Icon(
                    painter = painterResource(R.drawable.ic_payments),
                    contentDescription = null,
                    tint = SagayColors.DeepOceanDark,
                    modifier = Modifier.size(64.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            content.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = SagayColors.DeepOcean,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            content.body,
            style = MaterialTheme.typography.bodyLarge,
            color = SagayColors.InkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        when (page) {
            0 -> Text(
                "Tip: star a destination to find it faster next time.",
                style = MaterialTheme.typography.bodySmall,
                color = SagayColors.SecondaryTeal,
                textAlign = TextAlign.Center,
            )
            1 -> Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Illustrations only: these are the same colour codes the seed
                // data assigns to the real routes, never a redefinition of them.
                RouteBadge(VehicleType.TRICYCLE, "#D32F2F", size = 36.dp)
                RouteBadge(VehicleType.TRICYCLE, "#F9A825", size = 36.dp)
                RouteBadge(VehicleType.BUS, "#E91E63", size = 36.dp)
                RouteBadge(VehicleType.BOAT, "#FFFFFF", size = 36.dp)
            }
            else -> FilterChip(
                selected = true,
                onClick = {},
                enabled = false,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_payments),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = { Text("20% discount for eligible riders") },
            )
        }
    }
}
