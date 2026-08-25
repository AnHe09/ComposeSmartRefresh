package com.loren.component.view.composesmartrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import kotlinx.coroutines.delay

@Composable
fun rememberSmartSwipeRefreshState(): SmartSwipeRefreshState = remember { SmartSwipeRefreshState() }

/** SmartSwipeRefresh 的配置、运行状态和指示器动画容器。 */
class SmartSwipeRefreshState {
    var stickinessLevel = 0.5f
    var dragHeaderIndicatorStrategy: ThresholdScrollStrategy = ThresholdScrollStrategy.UnLimited
    var dragFooterIndicatorStrategy: ThresholdScrollStrategy = ThresholdScrollStrategy.UnLimited
    var flingHeaderIndicatorStrategy: ThresholdScrollStrategy = ThresholdScrollStrategy.None
    var flingFooterIndicatorStrategy: ThresholdScrollStrategy = ThresholdScrollStrategy.None
    var headerHeight = 0f
        internal set
    var footerHeight = 0f
        internal set
    var enableRefresh = true
    var enableLoadMore = true
    var enableAutoLoadMore = true
    /** 没有更多数据时，后续触底是否继续展示提示，默认开启。 */
    var showNoMoreData = true
    /** 没有更多数据提示展示后是否自动回弹，默认开启。 */
    var autoBackAfterNoMoreData = true
    var releaseIsEdge = false
        internal set
    var refreshFlag by mutableStateOf(SmartSwipeStateFlag.IDLE)
        internal set
    var loadMoreFlag by mutableStateOf(SmartSwipeStateFlag.IDLE)
        internal set
    var hasNoMoreData by mutableStateOf(false)
        private set
    var animateIsOver by mutableStateOf(true)
        internal set
    private val indicator = Animatable(0f)
    private val mutatorMutex = MutatorMutex()
    val indicatorOffset: Float get() = indicator.value
    fun isLoading() = !animateIsOver || refreshFlag == SmartSwipeStateFlag.REFRESHING || loadMoreFlag == SmartSwipeStateFlag.REFRESHING
    internal fun canLoadMore() = enableLoadMore && !hasNoMoreData && !isLoading()
    internal fun shouldShowNoMoreData() = hasNoMoreData && showNoMoreData && footerHeight != 0f
    internal fun startLoadMore(): Boolean {
        if (!canLoadMore()) return false
        loadMoreFlag = SmartSwipeStateFlag.REFRESHING
        return true
    }
    internal fun startRefresh() {
        hasNoMoreData = false
        if (loadMoreFlag == SmartSwipeStateFlag.NO_MORE) loadMoreFlag = SmartSwipeStateFlag.IDLE
        refreshFlag = SmartSwipeStateFlag.REFRESHING
    }
    internal fun completeRefresh(result: SmartSwipeResult) {
        refreshFlag = if (result == SmartSwipeResult.Error) SmartSwipeStateFlag.ERROR else SmartSwipeStateFlag.SUCCESS
    }
    internal fun completeLoadMore(result: SmartSwipeResult) {
        loadMoreFlag = when (result) {
            SmartSwipeResult.Success -> SmartSwipeStateFlag.SUCCESS
            SmartSwipeResult.Error -> SmartSwipeStateFlag.ERROR
            SmartSwipeResult.NoMore -> {
                hasNoMoreData = true
                animateIsOver = true
                SmartSwipeStateFlag.NO_MORE
            }
        }
    }
    internal suspend fun animateOffsetTo(offset: Float) {
        mutatorMutex.mutate {
            indicator.animateTo(offset) { if (value == 0f) animateIsOver = true }
        }
    }
    internal suspend fun showNoMoreData() {
        if (!shouldShowNoMoreData()) return
        loadMoreFlag = SmartSwipeStateFlag.NO_MORE
        animateIsOver = false
        try {
            mutatorMutex.mutate { indicator.animateTo(-footerHeight) }
            if (autoBackAfterNoMoreData) {
                delay(500)
                animateOffsetTo(0f)
            }
        } finally {
            animateIsOver = true
        }
    }
    internal suspend fun snapOffsetTo(offset: Float) {
        mutatorMutex.mutate(MutatePriority.UserInput) {
            indicator.snapTo(offset)
            if (indicatorOffset >= headerHeight) refreshFlag = SmartSwipeStateFlag.TIPS_RELEASE
            else if (indicatorOffset <= -footerHeight && !hasNoMoreData) loadMoreFlag = SmartSwipeStateFlag.TIPS_RELEASE
            else {
                if (indicatorOffset > 0) refreshFlag = SmartSwipeStateFlag.TIPS_DOWN
                if (indicatorOffset < 0 && !hasNoMoreData) loadMoreFlag = SmartSwipeStateFlag.TIPS_DOWN
            }
        }
    }
    internal suspend fun initRefresh() { snapOffsetTo(headerHeight); startRefresh() }
    internal fun strategyIndicatorHeight(strategy: ThresholdScrollStrategy): Float = when (strategy) {
        ThresholdScrollStrategy.None -> 0f
        is ThresholdScrollStrategy.Fixed -> strategy.height
        ThresholdScrollStrategy.UnLimited -> Float.MAX_VALUE
    }
}
