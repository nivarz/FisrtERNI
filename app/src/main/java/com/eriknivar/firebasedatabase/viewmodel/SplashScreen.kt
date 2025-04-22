package com.eriknivar.firebasedatabase.viewmodel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eriknivar.firebasedatabase.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController, userViewModel: UserViewModel) {
    val isLoggedOut = userViewModel.nombre.observeAsState("").value.isEmpty()
    val isInitialized = userViewModel.isInitialized.observeAsState(false).value

    val alpha = remember { Animatable(0f) }

    LaunchedEffect(isLoggedOut, isInitialized) {
        if (isInitialized) {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 1000))
            delay(600_000)

            if (isLoggedOut) {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("storagetype") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF547680)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logoernilupatransparente),
                contentDescription = null,
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer(alpha = alpha.value.coerceAtLeast(0.7f))

            )

            Spacer(modifier = Modifier.height(20.dp))

            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )
        }
    }
}

