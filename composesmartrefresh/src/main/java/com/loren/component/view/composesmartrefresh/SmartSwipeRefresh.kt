package com.loren.component.view.composesmartrefresh

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** 下拉刷新和上拉加载的 Compose 入口组件。 */
@Composable
fun SmartSwipeRefresh(
    modifier: Modifier = Modifier,
    state: SmartSwipeRefreshState = rememberSmartSwipeRefreshState(),
    onRefresh: (suspend () -> SmartSwipeResult)? = null,
    onLoadMore: (suspend () -> SmartSwipeResult)? = null,
    initialRefresh: Boolean = false,
    headerIndicator: @Composable (() -> Unit)? = { MyRefreshHeader(flag = state.refreshFlag) },
    footerIndicator: @Composable (() -> Unit)? = { MyRefreshFooter(flag = state.loadMoreFlag) },
    contentScrollState: ScrollableState? = null,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val connection = remember(coroutineScope, contentScrollState != null, onLoadMore != null) {
        SmartSwipeRefreshNestedScrollConnection(state, coroutineScope, contentScrollState != null, onLoadMore != null)
    }

    LaunchedEffect(state.refreshFlag) {
        when (state.refreshFlag) {
            SmartSwipeStateFlag.REFRESHING -> {
                state.animateIsOver = false
                state.completeRefresh(executeSwipeAction(onRefresh))
            }
            SmartSwipeStateFlag.ERROR, SmartSwipeStateFlag.SUCCESS -> {
                delay(500)
                state.animateOffsetTo(0f)
            }
            else -> Unit
        }
    }
    LaunchedEffect(state.loadMoreFlag) {
        when (state.loadMoreFlag) {
            SmartSwipeStateFlag.REFRESHING -> {
                state.animateIsOver = false
                state.completeLoadMore(executeSwipeAction(onLoadMore))
            }
            SmartSwipeStateFlag.ERROR, SmartSwipeStateFlag.SUCCESS -> {
                delay(500)
                state.animateOffsetTo(0f)
            }
            SmartSwipeStateFlag.NO_MORE -> if (state.autoBackAfterNoMoreData) {
                delay(500)
                state.animateOffsetTo(0f)
            }
            else -> Unit
        }
    }
    LaunchedEffect(Unit) {
        if (initialRefresh) state.initRefresh()
    }
    LaunchedEffect(state.indicatorOffset) {
        if (state.indicatorOffset < 0 && state.loadMoreFlag != SmartSwipeStateFlag.SUCCESS) {
            contentScrollState?.dispatchRawDelta(-state.indicatorOffset)
        }
    }

    Box(modifier.clipToBounds()) {
        SubComposeSmartSwipeRefresh(headerIndicator, footerIndicator) { header, footer ->
            state.headerHeight = header.toFloat()
            state.footerHeight = footer.toFloat()
            Box(Modifier.nestedScroll(connection)) {
                val offset = with(LocalDensity.current) { state.indicatorOffset.toDp() }
                val contentModifier = when {
                    offset > 0.dp -> Modifier.padding(top = offset)
                    offset < 0.dp && contentScrollState != null -> Modifier.padding(bottom = -offset)
                    offset < 0.dp -> Modifier.graphicsLayer { translationY = state.indicatorOffset }
                    else -> Modifier
                }
                Box(contentModifier) { content() }
                headerIndicator?.let {
                    Box(Modifier.align(Alignment.TopCenter).graphicsLayer { translationY = -header + state.indicatorOffset }) { it() }
                }
                footerIndicator?.let {
                    Box(Modifier.align(Alignment.BottomCenter).graphicsLayer { translationY = footer + state.indicatorOffset }) { it() }
                }
            }
        }
    }
}

private suspend fun executeSwipeAction(action: (suspend () -> SmartSwipeResult)?): SmartSwipeResult = try {
    action?.invoke() ?: SmartSwipeResult.Success
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    SmartSwipeResult.Error
}

enum class SmartSwipeStateFlag { IDLE, REFRESHING, SUCCESS, ERROR, NO_MORE, TIPS_DOWN, TIPS_RELEASE }

sealed interface SmartSwipeResult {
    data object Success : SmartSwipeResult
    data object Error : SmartSwipeResult
    data object NoMore : SmartSwipeResult
}

sealed interface ThresholdScrollStrategy {
    data object None : ThresholdScrollStrategy
    data object UnLimited : ThresholdScrollStrategy
    data class Fixed(val height: Float) : ThresholdScrollStrategy
}
