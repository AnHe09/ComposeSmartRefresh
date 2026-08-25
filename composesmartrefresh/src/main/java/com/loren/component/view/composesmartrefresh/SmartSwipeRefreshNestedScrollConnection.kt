package com.loren.component.view.composesmartrefresh

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

internal class SmartSwipeRefreshNestedScrollConnection(
    private val state: SmartSwipeRefreshState,
    private val coroutineScope: CoroutineScope,
    private val hasScrollableContent: Boolean,
    private val hasLoadMoreCallback: Boolean
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = when {
        state.isLoading() -> Offset.Zero
        available.y < 0 && state.indicatorOffset > 0 -> scroll((available.y * state.stickinessLevel).coerceAtLeast(-state.indicatorOffset))
        available.y > 0 && state.indicatorOffset < 0 -> scroll((available.y * state.stickinessLevel).coerceAtMost(-state.indicatorOffset))
        else -> Offset.Zero
    }
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = when {
        state.isLoading() -> Offset.Zero
        available.y < 0 && state.shouldShowNoMoreData() -> { coroutineScope.launch { state.showNoMoreData() }; available }
        available.y < 0 && hasScrollableContent && hasLoadMoreCallback && state.enableAutoLoadMore && (source == NestedScrollSource.Drag || source == NestedScrollSource.Fling) && state.startLoadMore() -> { coroutineScope.launch { state.animateOffsetTo(-state.footerHeight) }; available }
        available.y > 0 && state.enableRefresh && state.headerHeight != 0f -> {
            val max = if (source == NestedScrollSource.Fling) state.strategyIndicatorHeight(state.flingHeaderIndicatorStrategy) else state.strategyIndicatorHeight(state.dragHeaderIndicatorStrategy)
            scroll((available.y * state.stickinessLevel).coerceAtMost(max - state.indicatorOffset))
        }
        available.y < 0 && state.canLoadMore() && state.footerHeight != 0f -> {
            val max = if (source == NestedScrollSource.Fling) state.strategyIndicatorHeight(state.flingFooterIndicatorStrategy) else state.strategyIndicatorHeight(state.dragFooterIndicatorStrategy)
            scroll((available.y * state.stickinessLevel).coerceAtLeast(-max - state.indicatorOffset))
        }
        else -> Offset.Zero
    }
    private fun scroll(distance: Float): Offset = if (distance.absoluteValue > 0.5f) {
        coroutineScope.launch { state.snapOffsetTo(state.indicatorOffset + distance) }
        Offset(0f, distance / state.stickinessLevel)
    } else Offset.Zero
    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.isLoading()) return Velocity.Zero
        state.releaseIsEdge = state.indicatorOffset != 0f
        if (state.indicatorOffset >= state.headerHeight && state.releaseIsEdge && state.refreshFlag != SmartSwipeStateFlag.REFRESHING) {
            state.startRefresh(); state.animateOffsetTo(state.headerHeight); return available
        }
        if (state.indicatorOffset <= -state.footerHeight && state.releaseIsEdge && state.startLoadMore()) {
            state.animateOffsetTo(-state.footerHeight); return available
        }
        return super.onPreFling(available)
    }
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (state.isLoading()) return Velocity.Zero
        if (available.y < 0 && state.shouldShowNoMoreData()) { state.showNoMoreData(); return available }
        if (hasScrollableContent && hasLoadMoreCallback && available.y < 0 && state.enableAutoLoadMore && state.startLoadMore()) { state.animateOffsetTo(-state.footerHeight); return available }
        if (state.refreshFlag != SmartSwipeStateFlag.REFRESHING && state.indicatorOffset > 0) { state.refreshFlag = SmartSwipeStateFlag.IDLE; state.animateOffsetTo(0f) }
        if (state.loadMoreFlag != SmartSwipeStateFlag.REFRESHING && state.loadMoreFlag != SmartSwipeStateFlag.NO_MORE && state.indicatorOffset < 0) { state.loadMoreFlag = SmartSwipeStateFlag.IDLE; state.animateOffsetTo(0f) }
        return super.onPostFling(consumed, available)
    }
}
