package com.moneymanager.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A composable modifier that wraps any content in a Box and overlays a floating
 * scroll-to-top button when the user has scrolled past the given threshold.
 *
 * Usage:
 * ```kotlin
 * val lazyListState = rememberLazyListState()
 * Box(modifier = Modifier.fillMaxSize().scrollToTop(lazyListState)) {
 *     LazyColumn(state = lazyListState) { ... }
 * }
 * ```
 *
 * Or wrap the LazyColumn content:
 * ```kotlin
 * ScrollToTopBox(lazyListState = lazyListState) {
 *     LazyColumn(state = lazyListState) { ... }
 * }
 * ```
 */
@Composable
fun ScrollToTopBox(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    threshold: Int = 50,
    bottomMargin: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember(lazyListState) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > threshold
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = showScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomMargin),
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top"
                )
            }
        }
    }
}
