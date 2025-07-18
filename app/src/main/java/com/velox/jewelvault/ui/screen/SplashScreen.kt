package com.velox.jewelvault.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.velox.jewelvault.R
import com.velox.jewelvault.ui.nav.Screens
import com.velox.jewelvault.ui.theme.ZenFontFamily
import com.velox.jewelvault.utils.VaultPreview
import kotlinx.coroutines.delay


@Composable
@VaultPreview
fun SplashScreenPreview() {
    val navHost: NavHostController = rememberNavController()
    SplashScreen(navHost)
}

@Composable
fun SplashScreen(navHost: NavHostController) {

    val scale = remember { Animatable(0f) } // Start from 0 (not visible)

    LaunchedEffect(true) {
        // Smooth zoom-in over 4 seconds
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing)
        )
    }

    val title = "JEWEL VAULT"
    val visibleLetters = remember { mutableStateListOf<Char>() }

    // Start text drop after logo scale completes
    LaunchedEffect(Unit) {
        delay(4000)
        title.forEach { char ->
            visibleLetters.add(char)
            delay(120)
        }

        delay(1000)
       /* navHost.navigate(Screens.Login.route) { todo
            popUpTo(Screens.Splash.route) { inclusive = true }
        }*/
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo_1),
                contentDescription = "Splash Logo",
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value
                    )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(Modifier.height(200.dp)) {
                visibleLetters.forEach { letter ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 3800,
                                easing = LinearOutSlowInEasing
                            )
                        ) + slideInVertically(
                            animationSpec = tween(3800),
                            initialOffsetY = { fullHeight -> -fullHeight / 2 }
                        )
                    ) {
                        Text(
                            text = letter.toString(),
                            fontSize = 32.sp,
                            fontFamily = ZenFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

